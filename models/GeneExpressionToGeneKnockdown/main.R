library(bnlearn)
library(readr)
library(dplyr)

## read command line arguments
args <- commandArgs(trailingOnly = T)
query_file <- args[1]
output_file <- args[2]

## read model file
pgm_file <- "bnlearn.discrete.cmap.kd.a375.96h.Rdata"

## parse query file and load model
query <- read_tsv(query_file)
load(pgm_file)

query <- query %>% mutate(variableName = gsub("gene knockdown", "gene_knockdown", variableName))
input_pert <- query %>% filter(io == "input" & variableGroup == "GeneKnockdown" & probability > 0)
input_meas <- query %>% filter(io == "input" & variableGroup == "GeneExpression" & probability > 0)
output_template <- query %>% filter(io == "output") %>% mutate()

## prepare evidence list
evd <- list()
if(nrow(input_pert) > 0){
  evd[['gene_knockdown']] <- input_pert$variableValue
}

for(i in 1:nrow(input_meas)){
  evd[[input_meas$variableName[i]]] <- input_meas$variableValue[i]
}

## run query
query_dist <- cpdist(pgm, nodes = output_template$variableName, evidence = evd, method = "lw")

## query
output <- output_template[0,]
counter <- 1
for(i in 1:nrow(output_template)){
  var_dist <- table(query_dist[, output_template$variableName[i]])/nrow(query_dist)
  output <- rbind(output, output_template[rep(i, length(var_dist)),])
  for(j in 1:length(var_dist)){
    output$variableValue[counter] <- names(var_dist)[j]
    output$probability[counter] <- var_dist[j]
    counter <- counter + 1
  }
}

output <- output %>% mutate(variableName = gsub("gene_knockdown", "gene knockdown", variableName))

## write results
write.table(output, output_file, row.names = F, quote=F, sep = "\t")
