#!/usr/bin/env Rscript

library(jsonlite)
library(tidyjson)
library(dplyr)
library(readr)
library(tidyr)
library(rjson)
library(rjags)

parse_input <- function(json_string){
  # parse input specification with tidyjson
  input <- json_string %>%
    enter_object("modelInput") %>% 
    gather_array %>% 
    spread_values(variableGroupID = jstring("variableGroupID")) %>%
    enter_object("modelVariable") %>%
    gather_array %>%
    spread_values(variableID = jstring("variableID")) %>%
    enter_object("priorDistribution") %>%
    enter_object("discreteDistribution") %>%
    gather_array %>%
    spread_values(variableValue = jstring("variableValue"), priorProbability = jstring("priorProbability")) %>%
    select(-document.id, -array.index) %>%
    filter(priorProbability > 0)
  return(input)
}

parse_output_spec <- function(json_string){
  # parse output specification
  output_spec <- json_string %>%
    enter_object("modelOutput") %>%
    gather_array %>%
    spread_values(variableGroupID = jstring("variableGroupID"), rawOutput = jstring("rawOutput")) %>%
    enter_object("variableID") %>%
    gather_array %>%
    append_values_string("variableID") %>%
    mutate(variableID = gsub("gene knockdown", "gene_knockdown", variableID)) %>%
    select(-document.id, -array.index)
  return(output_spec)
}

process_query_results_gaussian <- function(query_dist, output_spec){
  ### process query results
  ## create list form results that can be written to JSON
  uGroup <- unique(output_spec$variableGroupID)
  output <- list(posteriorProbability = vector(mode = "list", length = length(uGroup)))
  for(i in 1:length(uGroup)){
    os_sub <- output_spec %>% filter(variableGroupID == uGroup[i])
    
    output$posteriorProbability[[i]]$variableGroupID <- uGroup[i]
    output$posteriorProbability[[i]]$modelVariable <- vector(mode = "list", length = nrow(os_sub))
    
    for(j in 1:nrow(os_sub)){
      if(uGroup[i] == "GeneExpression"){
        var_mean <- mean(query_dist[[1]][, os_sub$variableID[j]])
        var_sd <- sd(query_dist[[1]][, os_sub$variableID[j]])
        
        output$posteriorProbability[[i]]$modelVariable[[j]]$variableID = sub("^p_|^m_", "", os_sub$variableID[j])
        output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$GaussianDistribution$distributionMean <- var_mean
        output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$GaussianDistribution$distributionStDev <- var_sd
      } else if(uGroup[i] == "GeneKnockdown"){
        var_dist <- table(query_dist[[1]][, os_sub$variableID[j]])/nrow(query_dist[[1]])
        
        output$posteriorProbability[[i]]$modelVariable[[j]]$variableID = sub("^p_|^m_", "", os_sub$variableID[j])
        output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution <- list("discreteDistribution" = vector(mode = "list", length = length(var_dist)))
        
        for(k in 1:length(var_dist)){
          output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$discreteDistribution[[k]]$variableValue <- as.numeric(names(var_dist)[k])
          output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$discreteDistribution[[k]]$posteriorProbability <- var_dist[[k]]
        }
      } else {
        stop(paste("Unknown variable group:", uGroup[i]))
      }
    }
  }
  
  return(output)
}


## read command line arguments
args <- commandArgs(trailingOnly = T)
query_file <- args[1]
output_file <- args[2]

## read model file
pgm_file <- "jags.linear.gaussian.cmap.kd.a375.96h.Rdata"

# read JSON with jsonlite
json_string <- read_file(query_file)

# parse input and output spec
input <- parse_input(json_string)
output_spec <- parse_output_spec(json_string)

# add prefix
input <- input %>% mutate(variableID = ifelse(variableGroupID == "GeneExpression", paste0("m_", variableID), variableID)) %>%
  mutate(variableID = ifelse(variableGroupID == "GeneKnockdown", paste0("p_", variableID), variableID))

output_spec <- output_spec %>% mutate(variableID = ifelse(variableGroupID == "GeneExpression", paste0("m_", variableID), variableID)) %>%
  mutate(variableID = ifelse(variableGroupID == "GeneKnockdown", paste0("p_", variableID), variableID))

### calculate posterior distribution
## load model
load(pgm_file)

## run query
# prepare evidence
meas_obs <- list()
if(nrow(input) > 0){
  for(i in 1:nrow(input)){
    meas_obs[[input$variableID[i]]] <- input$variableValue[i]
  }
}

# set data
data <- meas_obs

# set model
model <- jags.model(textConnection(modelstring), data=data)

# run model
update(model,n.iter=10)

# get results
query_dist <- coda.samples(model=model,variable.names=output_spec$variableID, n.iter=100, thin=1)


## process query results
output <- process_query_results_gaussian(query_dist, output_spec)

## write JSON
write(prettify(rjson::toJSON(output)), file = output_file)