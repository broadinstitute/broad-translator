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

#read EHR data from the config file
$configFileName = "$path/ehrToPhenotype.config.txt";
open(IN, $configFileName) || die "Cannot open the config file: $configFileName\n";
$header = "";
$header = <IN>;
while(<IN>) {
	chomp;
	@data = split /\t/, $_;
	die "Config file format error: @data\n" if @data !=3 && @data != 5;
	$ehr2meanCtrl{$data[0]} = $data[1] + 0; 
	$ehr2meanDiff{$data[0]} = $data[2] - $data[1];
	next if @data == 3; 
	$ehr2stdvCtrl{$data[0]} = $data[3] + 0; 
	$ehr2stdvDiff{$data[0]} = $data[4] - $data[3];
}

#open input json file, read it, and put data into jsonInputData
open(IN, $inputFileName) || die "Cannot read input file: $inputFileName\n";
my $jsonInputString = "";
while(<IN>) {
	chomp;
	$jsonInputString .= $_;	
}
$jsonInputData = decode_json($jsonInputString);

#Analyze modelOutput definition in jsonInputData
die "No modelOutput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelOutput"};
foreach(@{$jsonInputData->{"modelOutput"}}) {
	next if !exists $_->{"variableID"};
	foreach(@{$_->{"variableID"}}) {
		push @phenos, $_;
	}
}
die "Wrong phenotypes provided: @phenos\n" if @phenos != 1 && $phenos[0] ne "type-2 diabetes";

#Check if prevalnce ws provided in jsonInputData
$prevalence = "";
die "No modelInput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelInput"};
foreach(@{$jsonInputData->{"modelInput"}}) {
	next if !exists         $_->{"modelVariable"};
	next if !exists         $_->{"variableGroupID"};
	next if "Phenotypes" ne $_->{"variableGroupID"};
	foreach my $ehrJson (@{$_->{"modelVariable"}}) {
		die "modelVariable format error: missing variableID\n"               if !exists $ehrJson->{"variableID"};
		die "Prevalence provided for other phenotype than type-2 diabetes\n" if         $ehrJson->{"variableID"} ne "type-2 diabetes";
		die "modelVariable format error: missing priorDistribution\n"        if !exists $ehrJson->{"priorDistribution"};
		die "modelVariable format error: missing scalarValue\n"              if !exists $ehrJson->{"priorDistribution"}->{"discreteDistribution"};
		die "modelVariable format error: number of discrete <> 1\n"          if  1 != @{$ehrJson->{"priorDistribution"}->{"discreteDistribution"}};
		die "modelVariable format error: missing variableValue\n"            if !exists $ehrJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};
		die "modelVariable format error: variableValue should be = 1\n\n"    if  1 !=   $ehrJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};
		die "modelVariable format error: missing priorProbability\n"         if !exists $ehrJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};
		die "Prevalence was already set to be = $prevalence\n"               if $prevalence ne "";
		$prevalence = $ehrJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};
	}
}
$prevalence = 0.09 if $prevalence eq "";

#Analyze modelInput definition in jsonInputData
die "No modelInput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelInput"};
foreach(@{$jsonInputData->{"modelInput"}}) {
	next if !exists   $_->{"modelVariable"};
	next if !exists   $_->{"variableGroupID"};
	next if "EHR" ne  $_->{"variableGroupID"};
	foreach my $ehrJson (@{$_->{"modelVariable"}}) {
		die "modelVariable format error: missing variableID\n"        if !exists $ehrJson->{"variableID"};
		die "modelVariable format error: missing priorDistribution\n" if !exists $ehrJson->{"priorDistribution"};
		die "modelVariable format error: missing scalarValue\n"       if !exists $ehrJson->{"priorDistribution"}->{"scalarValue"};
		die "modelVariable format error: missing variableValue\n"     if !exists $ehrJson->{"priorDistribution"}->{"scalarValue"}->{"variableValue"};
		my $ehr = $ehrJson->{"variableID"}; 
		die "No data found in the EHR config file for: $ehr\n" if !exists $ehr2meanCtrl{$ehr};
		die "Multiple input records for ehr: $ehr" if exists $ehr2value{$ehr};
		if($ehr eq "Gender") {
			   if($ehrJson->{"priorDistribution"}->{"scalarValue"}->{"variableValue"} eq "M") { $ehr2value{$ehr} = 1 }
			elsif($ehrJson->{"priorDistribution"}->{"scalarValue"}->{"variableValue"} eq "F") { $ehr2value{$ehr} = 0 }
			else { die "Unrecognized Gender label: ",$ehrJson->{"priorDistribution"}->{"scalarValue"}->{"variableValue"}, "\n" }
		}
		else {
			$ehr2value{$ehr} = $ehrJson->{"priorDistribution"}->{"scalarValue"}->{"variableValue"};
		}
	}
}
@ehrs = sort keys %ehr2value; 

#generate R code and run sampling
open3(INR, OTR, ERR, "R --no-save --slave | tail -1") || die "Unable to run R\n";

$modelCodeInR  = "";
$modelCodeInR .= "library(coda)\n";
$modelCodeInR .= "library(rjags)\n";

#print code for model definition
$modelCodeInR .= "model = \"model {\n";
foreach $ehr (@ehrs) {
	if(!exists $ehr2stdvCtrl{$ehr}) { $modelCodeInR .= "\t$ehr ~ dbern($ehr2meanCtrl{$ehr} + phenoProb * $ehr2meanDiff{$ehr}) \n" }
	else                            { $modelCodeInR .= "\t$ehr ~ dnorm($ehr2meanCtrl{$ehr} + phenoProb * $ehr2meanDiff{$ehr}, \n" ;
	                                  $modelCodeInR .= "\t\t\t\t1/sqrt($ehr2stdvCtrl{$ehr} + phenoProb * $ehr2stdvDiff{$ehr}))\n" }
}
$modelCodeInR .=                             "\tphenoProb ~ dbern($prevalence)\n";
$modelCodeInR .= "}\"\n";

#print code for defining input data
$modelCodeInR .= "data <- list(";
for($i=0; $i < @ehrs; $i++) {
	$modelCodeInR .= ($i?", ":"")."'$ehrs[$i]' = $ehr2value{$ehrs[$i]}";
}
$modelCodeInR .= ")\n";

#print code for sampling
$modelCodeInR .= "jags <- jags.model(textConnection(model), data = data, n.chains = 3 )\n";
$modelCodeInR .= "update(jags, 5000)\n";
$modelCodeInR .= "results <- coda.samples(jags,c('phenoProb'), 20000)\n";
$modelCodeInR .= "summary <- summary(results)\n";
$modelCodeInR .= "cat(summary\$statistics[\"Mean\"])\n";

print INR $modelCodeInR;
close(INR);

#read sampling results
$phenoProb = <OTR>;
close(OTR);
if(!defined $phenoProb) {
	print STDERR "Error while running the model:\n";
	while(<ERR>) {
		print STDERR $_;
	}
	print STDERR "\nThe code in R was as follow:\n$modelCodeInR";
	close(ERR);
	exit;
}

$jsonOutputData->{"posteriorProbability"}[0]->{"variableGroupID"} = "Phenotypes";
$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[0]->{"variableID"} = $phenos[0];
$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[0]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"} = 1;
$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[0]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"posteriorProbability"} = 0 + sprintf "%.1e", $phenoProb;

#open output file and and right the jsonOutput
open(OT,">$outputFileName") || die "Cannot open output file: $outputFileName\n";
my $json = JSON->new->ascii->pretty->allow_nonref;
print OT $json->encode($jsonOutputData);
