#!/usr/bin/perl -w

die "Usage $0 inputFile.txt outputFile.txt\n" if @ARGV!=2;

use Cwd 'abs_path';
use File::Basename;
my $path = dirname(abs_path($0));

#define input output file names
$inputFileName  = $ARGV[0];
$outputFileName = $ARGV[1];

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

	chomp($line = $_);
	@data = split /\t/, $line;

	if($data[0] eq "input" && $data[1] eq "phenotype") {
		die "Unrecognized phenotype: $data[2]\n" if !exists $ph2fn{$data[2]};
		$phenos{$data[2]} = 1;
		print OT $line,"\n";
	}
	elsif($data[0] eq "output" && $data[1] eq "variant") {
		die "Unrecognized variant ID: $data[2]\n" if !exists $vid2eaf{$data[2]};
		push @vars, $data[2];
		push @output, $line;
	}
	else {
		die "Input file format error in line: $line\n";
	}
}

#read Betas for all phenotypes
@phenos = sort keys %phenos;
foreach $ph (@phenos) {
	open(IN, "cut -f1,2 $ph2fn{$ph} |") || die "Cannot read Beats from: $ph2fn{$ph}\n";
	while(<IN>) {
		chomp;
		($vid, $beta) = split /\t/, $_;
		next if !exists $vid2prob{$vid};
		$ph2vid2beta{$ph}{$vid} = $beta;
	}
}

#put zeros of unkown pheno-beta pairs
foreach $ph (@phenos) {
	foreach $vid (@vars) {
		$ph2vid2beta{$ph}{$vid} = 0 if !exists $ph2vid2beta{$ph}{$vid};
	}
}

foreach(keys %vid2prob) { $vid2prob{$_}=$vid2eaf{$_} }

#print the main (rest) part of the output
$i=0;
foreach(@output) { 
	print OT $_,"\t",$vid2prob{$vars[$i++]},"\n"
}
