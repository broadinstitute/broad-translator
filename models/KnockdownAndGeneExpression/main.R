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

input_pert <- query %>% filter(io == "input" & variableGroup == "GenePerturbed" & probability > 0)
input_meas <- query %>% filter(io == "input" & variableGroup == "GeneMeasured" & probability > 0)
output_template <- query %>% filter(io == "output")

## prepare evidence list
evd <- list()
if(nrow(input_pert) > 0){
  evd[['pert']] <- input_pert$variableValue
}

for(i in 1:nrow(input_meas)){
  evd[[input_meas$variableName[i]]] <- input_meas$variableValue[i]
}

## run query
query_dist <- cpdist(pgm, nodes = output_template$variableName, evidence = evd, method = "lw")

## extract results
output <- output_template[0,]
counter <- 1
for(i in 1:nrow(output_template)){
  var_dist <- table(query_dist[,output_template$variableName[i]])/nrow(query_dist)
  output <- rbind(output, output_template[rep(i, length(var_dist)),])
  for(j in 1:length(var_dist)){
    output$variableValue[counter] <- names(var_dist)[j]
    output$probability[counter] <- var_dist[j]
    counter <- counter + 1
  }
}

## write results
write.table(output, output_file, row.names = F, quote=F, sep = "\t")
