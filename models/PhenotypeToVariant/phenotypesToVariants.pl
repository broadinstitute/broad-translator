#!/usr/bin/perl -w

die "Usage $0 inputFile.txt outputFile.txt\n" if @ARGV!=2;

use IPC::Open3;
use Cwd 'abs_path';
use File::Basename;
my $path = dirname(abs_path($0));

#define input output file names
$inputFileName  = $ARGV[0];
$outputFileName = $ARGV[1];

#read phenotypes IDs to names dictionary
$dictionaryFileName = "$path/GWAS/phenotypesDictionary.txt";
open(IN, $dictionaryFileName) || die "Cannot open phenotype dictionary file: $dictionaryFileName\n";
while(<IN>) {
	chomp;
	($phid, $name) = split /\t/, $_;
	$name2phid{$name} = $phid;
}

#define list of known phenptypes
$ph2fn{"CAD"} = "$path/GWAS/GWAS_CARDIoGRAM_dv2.CAD.1000GP_AF.txt";
$ph2fn{"T2D"} = "$path/GWAS/GWAS_DIAGRAM_eu_dv2.T2D.1000GP_AF.txt";
$ph2fn{"BIP"} = "$path/GWAS/GWAS_PGC_dv1.BIP.1000GP_AF.txt";
$ph2fn{"MDD"} = "$path/GWAS/GWAS_PGC_dv1.MDD.1000GP_AF.txt";
$ph2fn{"SCZ"} = "$path/GWAS/GWAS_PGC_dv1.SCZ.1000GP_AF.txt";

#read var IDs from input
open(IN, "grep ^output $inputFileName | cut -f3 |") || die "Cannot read variants IDs from input file: $inputFileName\n";
while(<IN>) {
	chomp;
	$vid2prob{$_} = "";
}

#read EAFs
$files="";
foreach(keys %ph2fn) { $files.="$ph2fn{$_} " }
open(IN, "cut -f1,3 $files | sort -u |") || die "Cannot read EAFs\n";
while(<IN>) {
	chomp;
	($vid, $eaf) = split /\t/, $_;
	next if !exists $vid2prob{$vid};
	$vid2eaf{$vid} = $eaf;
}

#open input/output files for reading and writing
open(IN,   $inputFileName ) || die "Cannot open  input file: $ARGV[0]\n";
open(OT,">$outputFileName") || die "Cannot open output file: $ARGV[1]\n";

#read/validate/write header
chomp($header=<IN>);
die "Header format error: $header" if $header ne "io\tvariableGroup\tvariableName\tvariableValue\tprobability";
print OT $header,"\n";

#read input file and start generating output file
while(<IN>) {

	next if /^#/;
	chomp($line = $_);
	$line=~s/\t$//;
	$line=~s/\t$//;
	@data = split /\t/, $line;

	if($data[0] eq "input" && $data[1] eq "Phenotype") {
		die "Input format error for: $line" if @data != 5;
		die "No phenotype translation found in phenotype dictionary for: $data[2]\n" if !exists $name2phid{$data[2]};
		$phid = $name2phid{$data[2]};
		die "No data file found for  henotype: $data[2]\n" if !exists $ph2fn{$phid};
		die "Multiple input records for phenotype: $data[2]" if exists $pheno2prob{$phid};
		$pheno2prob{$phid} = $data[4];
		push @phenos, $phid;
		print OT $line,"\n";
	}
	elsif($data[0] eq "output" && $data[1] eq "Variant") {
		die "Input format error for: $line" if @data != 3;
		die "Unrecognized variant ID: $data[2]\n" if !exists $vid2eaf{$data[2]};
		push @vars, $data[2];
		push @output, $line;
	}
	else {
		die "Input file format error in line: $line\n";
	}
}
$phenosN = @phenos;
$varsN   = @vars;

#read Betas for all phenotypes
foreach $ph (@phenos) {
	open(IN, "cut -f1,2 $ph2fn{$ph} |") || die "Cannot read Beats from: $ph2fn{$ph}\n";
	while(<IN>) {
		chomp;
		($vid, $beta) = split /\t/, $_;
		next if !exists $vid2prob{$vid};
		$ph2vid2beta{$ph}{$vid} = $beta;
	}
}

#prepare input data for R code
foreach $ph (@phenos) {
	$prevs .= (!defined $prevs ? "" : ", ").$pheno2prob{$ph};
}

foreach $vid (@vars) {
	$freqs .= (!defined $freqs? "" : ", ").$vid2eaf{$vid};
}

foreach $vid (@vars) {
	foreach $ph (@phenos) {
		$betas .= (!defined $betas? "" : ", ").(exists $ph2vid2beta{$ph}{$vid} ? $ph2vid2beta{$ph}{$vid} : 0);	
	}
}

#generate R code and run sampling
open3(INR, OTR, ERR, "R --no-save --slave | tail -1") || die "Unable to run R\n";
print INR <<END;
library(coda)
library(rjags)

M <- $phenosN
prev <- c($prevs)

N <- $varsN
freq <- c($freqs)
beta <- matrix(c($betas), nrow=N, ncol=M, byrow=TRUE)

data <- list('M'=M, 'prev'=prev, 'N'=N, 'freq'=freq, 'beta'=beta)

burnin <-  100
steps  <- 1000

model = "model {
	for(i in 1:N) {
		vars[i] ~ dbern(1 / (1 + (1 - freq[i]) / (freq[i] * exp(btsum[i]))))
		btsum[i] <- sum(beta[i,] * pheno)
	}
	for(i in 1:M) {
		pheno[i] ~ dbern(prev[i])
	}
}"

jags <- jags.model(textConnection(model), data = data, n.chains = 3 )
update(jags, burnin)
results <- coda.samples(jags,c('vars'),steps)

cat(summary(results)\$statistics[,"Mean"])
END
close(INR);

#read sampling results
$probs = <OTR>;
close(OTR);
if(!defined $probs) {
	while(<ERR>) { print STDERR $_ }
	close(ERR);
	exit;
}
$probs =~ s/^\s+//;
@probs = split / /, $probs;

#print the main (rest) part of the output
$i=0;
foreach(@output) { 
	print OT $_,"\t1\t",(sprintf "%1.1e",$probs[$i++]),"\n"
}
