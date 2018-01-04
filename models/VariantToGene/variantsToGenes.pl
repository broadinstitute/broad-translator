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

#program variables
$maxProbParam = 1.5;
$betaParam    = 1.0;
$maxDist   = 250000;

#open input json file, read it, and put data into jsonInputData
open(IN, $inputFileName) || die "Cannot read input file: $inputFileName\n";
my $jsonInputString = "";
while(<IN>) {
	chomp;
	$jsonInputString .= $_;
}
my $jsonInputData = decode_json($jsonInputString);

#analyze modelInput definition in jsonInputData
$varInx = 0;
die "No modelInput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelInput"};
foreach(@{$jsonInputData->{"modelInput"}}) {
	next if !exists $_->{"modelVariable"};
	foreach my $varJson (@{$_->{"modelVariable"}}) {
		die "modelVariable format error: missing variableID\n"           if !exists $varJson->{"variableID"};
		die "modelVariable format error: missing priorDistribution\n"    if !exists $varJson->{"priorDistribution"};
		die "modelVariable format error: missing discreteDistribution\n" if !exists $varJson->{"priorDistribution"}->{"discreteDistribution"};
		die "modelVariable format error: number of discrete <> 1\n"      if  1 != @{$varJson->{"priorDistribution"}->{"discreteDistribution"}};
		die "modelVariable format error: missing variableValue\n"        if !exists $varJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};
		die "modelVariable format error: missing priorProbability\n"     if !exists $varJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};
		die "modelVariable format error: variableValue should be = 1\n"  if  1 !=   $varJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"};

		my $var = $varJson->{"variableID"};
		die "Multiple input records for variant: $var" if exists $var2prob{$var};
		$var2prob{$var} = $varJson->{"priorDistribution"}->{"discreteDistribution"}[0]->{"priorProbability"};

		$var=~/^([^_]+)_(\d+)/ || die "Variant ID format error: $_";
		push @tmp_chr, $1;
		push @tmp_pos, $2;
		push @tmp_var, $var;
		push @inx    , $varInx++;
	}
}
$varsN = keys %var2prob;
die "No variant ID found in modelInput definition in JSON input file\n" if $varsN == 0;

#analyze modelOutput definition in jsonInputData
die "No modelOutput definition in JSON input file\n"  if !exists   $jsonInputData->{"modelOutput"};
foreach(@{$jsonInputData->{"modelOutput"}}) {
	next if !exists $_->{"variableID"};
	foreach(@{$_->{"variableID"}}) {
		$gene2prob{$_} = 0;
	}
}
$genesN = keys %gene2prob;
die "No variant ID found in modelOutput definition in JSON input file\n" if $genesN == 0;

#reorganized input data by sorting it by chr and pos
sub cmp_chr {
	if($_[0]=~/^\d+$/) { $_[1]!~/^\d+$/ ? return -1 : return $_[0] <=> $_[1] }
	else               { $_[1]=~/^\d+$/ ? return  1 : return $_[0] cmp $_[1] }
}

foreach $inx (sort { &cmp_chr($tmp_chr[$a],$tmp_chr[$b]) || $tmp_pos[$a]<=>$tmp_pos[$b] || $tmp_var[$a] cmp $tmp_var[$b] } @inx) {
	push @chr, $tmp_chr[$inx];
	push @pos, $tmp_pos[$inx];
	push @var, $tmp_var[$inx];
}

#find starting and ending index for each chromosome
if(@chr>0) {
	$chr2i{$chr[     0]}=   0;
	$chr2n{$chr[@chr-1]}=@chr;
}
for($i=1;$i<@chr;$i++) {
	if($chr[$i-1] ne $chr[$i]) {
		$chr2n{$chr[$i-1]} = $i;
		$chr2i{$chr[$i  ]} = $i;
	}
}

#read gene list and find all variants for each gene
$configFileName = "$path/variantsToGenes.config.txt";
open(IN, $configFileName) || die "Cannot open the config file: $configFileName\n";
while(<IN>) {
	chomp;
	($gene,$chr,$str,$end)=split /\t/, $_;
	next if !exists $chr2i{$chr};
	next if !exists $gene2prob{$gene};
	for($i=$chr2i{$chr}; $i < $chr2n{$chr}; $i++) {
		next if $str - $pos[$i] > $maxDist;
		last if $pos[$i] - $end > $maxDist;
		if($str < $pos[$i]) {
			$dist = $pos[$i] < $end ? 0 : $end - $pos[$i];
		}
		else { $dist = $str - $pos[$i] }
		$genes2vars{$gene}{$var[$i]} = $dist;
		if(!exists $gene2chr{$gene}) {
			$gene2chr{$gene} = $chr;
			$gene2pos{$gene} = ($str + $end)/2;
		}
	}
}

#estimate probability for each gene
foreach $gene (sort { &cmp_chr($gene2chr{$a},$gene2chr{$b}) || $gene2pos{$a} <=> $gene2pos{$b} || $a cmp $b } keys %genes2vars) {

	$N=0;
	$prev="";
	$dist="";
	foreach $var (sort { $genes2vars{$gene}{$a} <=> $genes2vars{$gene}{$b} || $a cmp $b } keys %{$genes2vars{$gene}}) {
		die if !exists $var2prob{$var};
		$prev .= ($prev eq ""?"":",").$var2prob{$var};
		$dist .= ($dist eq ""?"":",").$genes2vars{$gene}{$var};
		$N++;
	}

	#generate R code and run sampling
	open3(INR, OTR, ERR, "R --no-save --slave | tail -1") || die "Unable to run R\n";
	
	$modelCodeInR   = "";
	$modelCodeInR  .= "library(coda)\n";
	$modelCodeInR  .= "library(rjags)\n";
	$modelCodeInR  .= "model = \"model {\n";
	$modelCodeInR  .= "  geneProb ~ dbern(function)\n";
	$modelCodeInR  .= "  function <- 1 - exp(-$betaParam * x)\n";
	$modelCodeInR  .= "  x <- sum(varProb*($maxProbParam * (1 - dist / $maxDist)))\n";
	$modelCodeInR  .= "  for(i in 1:N) {\n";
	$modelCodeInR  .= "    varProb[i] ~ dbern(prev[i])\n";
	$modelCodeInR  .= "  }\n";
	$modelCodeInR  .= "}\"\n";
	$modelCodeInR  .= "data <- list('N' = $N, 'prev' = c($prev), 'dist' = c($dist))\n";
	$modelCodeInR  .= "jags <- jags.model(textConnection(model), data = data, n.chains = 3 )\n";
	$modelCodeInR  .= "update(jags, 5000)\n";
	$modelCodeInR  .= "results <- coda.samples(jags,c('geneProb'), 20000)\n";
	$modelCodeInR  .= "summary <- summary(results)\n";
	$modelCodeInR  .= "cat(summary\$statistics[\"Mean\"])\n";

	print INR $modelCodeInR;
	close(INR);
	
	#read sampling results
	$geneProb = <OTR>;
	close(OTR);
	if(!defined $geneProb) {
		print STDERR "Error while running the model:\n";
		while(<ERR>) {
			print STDERR $_;
		}
		print STDERR "\nThe code in R was as follow:\n$modelCodeInR";
		close(ERR);
		next;
	}

	$gene2prob{$gene} = $geneProb;
}

#prepare jsonOutput;
$geneInx = 0;
foreach $gene (keys %gene2prob) {
	$jsonOutputData->{"posteriorProbability"}[0]->{"variableGroupID"} = "Genes";
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$geneInx]->{"variableID"} = $gene;
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$geneInx]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"variableValue"} = 1;
	$jsonOutputData->{"posteriorProbability"}[0]->{"modelVariable"}[$geneInx]->{"posteriorDistribution"}->{"discreteDistribution"}[0]->{"posteriorProbability"} = 0 + sprintf "%.1e", $gene2prob{$gene};
	$geneInx++;
}

#open output file and wright jsonOutput
open(OT,">$outputFileName") || die "Cannot open output file: $outputFileName\n";
my $json = JSON->new->ascii->pretty->allow_nonref;
print OT $json->encode($jsonOutputData);
