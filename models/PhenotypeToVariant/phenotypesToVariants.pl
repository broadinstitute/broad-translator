#!/usr/bin/perl -w

die "Usage: perl $0 inputFile.json outputFile.json\n" if @ARGV!=2;

use JSON;
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

#open input json file, read it, and put data into jsonInputData
open(IN, $inputFileName) || die "Cannot read input file: $inputFileName\n";
my $jsonInputString = "";
while(<IN>) {
	chomp;
	$jsonInputString .= $_;	
}
my $jsonInputData = decode_json($jsonInputString);

#Analyze modelOutput definition in jsonInputData
die "No modelOutput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelOutput"};
foreach(@{$jsonInputData->{"modelOutput"}}) {
	next if !exists $_->{"variableID"};
	foreach(@{$_->{"variableID"}}) {
		push @vars, $_;
		$vid2prob{$_} = "";
	}
}
$varsN = @vars;
die "No variant ID found in modelOutput definition in in JSON input file\n" if $varsN == 0;

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

#Analyze modelInput definition in jsonInputData
die "No modelInput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelInput"};
foreach(@{$jsonInputData->{"modelInput"}}) {
	next if !exists $_->{"modelVariable"};
	foreach my $phenoJson (@{$_->{"modelVariable"}}) {
		die "modelVariable format error: missing variableID\n"           if !exists $phenoJson->{"variableID"};
		die "modelVariable format error: missing priorDistribution\n"    if !exists $phenoJson->{"priorDistribution"};
		die "modelVariable format error: missing discreteDistribution\n" if !exists $phenoJson->{"priorDistribution"}->{"discreteDistribution"};
		die "modelVariable format error: number of discrete <> 1\n"      if  1 != @{$phenoJson->{"priorDistribution"}->{"discreteDistribution"}};
		die "modelVariable format error: missing variableValue\n"        if !exists $phenoJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};
		die "modelVariable format error: missing priorProbability\n"     if !exists $phenoJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};
		die "modelVariable format error: variableValue should be = 1\n"  if  1 !=   $phenoJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};
		my $phenoName = $phenoJson->{"variableID"}; 
		die "No phenotype translation found in phenotype dictionary for: $phenoName\n" if !exists $name2phid{$phenoName};
		$phid = $name2phid{$phenoName};	
		die "No data file found for henotype: $phenoName\n" if !exists $ph2fn{$phid};
		die "Multiple input records for phenotype: $phenoName" if exists $pheno2prob{$phid};
		$pheno2prob{$phid} = $phenoJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};
		push @phenos, $phid;
	}
}
$phenosN = @phenos;

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

burnin <-  500
steps  <- 5000

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

$jsonOutputData->{"posteriorProbability"}[0]->{"variableGroupID"} = "Variants";
for($i=0; $i<@vars; $i++) {
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$i]->{"variableID"} = $vars[$i];
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$i]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"} = 1;
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$i]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"posteriorProbability"} = 0 + sprintf "%.1e", $probs[$i];
}

#open output file and and right the jsonOutput
open(OT,">$outputFileName") || die "Cannot open output file: $outputFileName\n";
my $json = JSON->new->ascii->pretty->allow_nonref;
print OT $json->encode($jsonOutputData);
