#!/usr/bin/env Rscript

library(jsonlite)
library(dplyr)
library(readr)
library(tidyr)
library(stringr)
library(rjson)
library(rjags)

get_model_id <- function(json_object){
  return(json_object$modelID)
}

parse_input <- function(json_object){
  # parse input
  nvars <- Reduce(sum, sapply(json_object$modelInput, function(x) length(x$modelVariable)))
  parsed <- data_frame(variableGroupID = vector(mode = "character", length = nvars), variableID = NA, variableValue = NA)
  
  counter <- 1
  for(ugroup in json_object$modelInput){
    for(mvar in ugroup$modelVariable){
      parsed[counter,"variableGroupID"] <- ugroup$variableGroupID
      parsed[counter,"variableID"] <-  mvar$variableID
      parsed[counter,"variableValue"] <- mvar$priorDistribution$scalarValue$variableValue
      counter = counter + 1
    }
  }
  return(parsed)
}

parse_output_spec <- function(json_object){
  # parse output specification
  nvars <- Reduce(sum, sapply(json_object$modelOutput, function(x) length(x$variableID)))
  parsed <- data_frame(variableGroupID = vector(mode = "character", length = nvars), variableID = "", rawOutput = FALSE)
  
  counter <- 1
  for(ugroup in json_object$modelOutput){
    for(var_id in ugroup$variableID){
      parsed[counter,"variableGroupID"] <- ugroup$variableGroupID
      parsed[counter,"variableID"] <-  var_id
      parsed[counter,"rawOutput"] <-  ugroup$rawOutput
      counter = counter + 1
    }
  }
  return(parsed)
}

process_query_results <- function(query_dist, output_spec, vmap = NULL, discrete = F){
  ### process query results
  ## create list form results that can be written to JSON
  uGroup <- unique(output_spec$variableGroupID)
  output <- list(posteriorProbability = vector(mode = "list", length = length(uGroup)))
  for(i in 1:length(uGroup)){
    os_sub <- output_spec %>% filter(variableGroupID == uGroup[i])
    
    output$posteriorProbability[[i]]$variableGroupID <- uGroup[i]
    output$posteriorProbability[[i]]$modelVariable <- vector(mode = "list", length = nrow(os_sub))
    
    for(j in 1:nrow(os_sub)){
      if(uGroup[i] == "GeneExpression" & discrete == F){
        var_mean <- mean(query_dist[[1]][, os_sub$variableID[j]])
        var_sd <- sd(query_dist[[1]][, os_sub$variableID[j]])
        
        output$posteriorProbability[[i]]$modelVariable[[j]]$variableID = sub("^p_|^m_", "", os_sub$variableID[j])
        output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$GaussianDistribution$distributionMean <- var_mean
        output$posteriorProbability[[i]]$modelVariable[[j]]$posteriorDistribution$GaussianDistribution$distributionStDev <- var_sd
      } else if(uGroup[i] == "GeneKnockdown" | uGroup[i] == "CompoundTreatment" | discrete == T){
        var_dist <- table(query_dist[[1]][, os_sub$variableID[j]])/nrow(query_dist[[1]])
        
        # if(!is.null(vmap)){
        #   ## decode output variables
        #   names(var_dist) <- vmap[[uGroup[i]]][as.numeric(names(var_dist))]
        # }
        
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

encode_input <- function(input, vmap){
  for(vgroup in names(vmap)){
    if(vgroup %in% input$variableGroupID){
      input <- input %>% mutate(variableValue = ifelse(variableGroupID == vgroup, as.integer(factor(variableValue, levels = vmap[[unique(variableGroupID)]])), variableValue))
    }
  }
  return(input)
}


## read command line arguments
args <- commandArgs(trailingOnly = T)
pgm_file <- args[1]
query_file <- args[2]
output_file <- args[3]

# read JSON with jsonlite
json_string <- read_file(query_file)
json_object <- fromJSON(json_string)

# get model ID
model_id <- get_model_id(json_object)

# parse input and output spec
input <- parse_input(json_object)
output_spec <- parse_output_spec(json_object)


if(model_id %in% c("GeneKnockdownAndGeneExpression", "GeneKnockdownToGeneExpression", "GeneExpressionToGeneKnockdown", "GeneKnockdownAndDiscreteGeneExpression", "DiscreteGeneExpressionToGeneKnockdown", "GeneKnockdownToDiscreteGeneExpression")){
  # add prefix
  input <- input %>% mutate(variableID = ifelse(variableGroupID == "GeneExpression", paste0("m_", variableID), variableID)) %>%
    mutate(variableID = ifelse(variableGroupID == "GeneKnockdown", paste0("p_", variableID), variableID))
  
  output_spec <- output_spec %>% mutate(variableID = ifelse(variableGroupID == "GeneExpression", paste0("m_", variableID), variableID)) %>%
    mutate(variableID = ifelse(variableGroupID == "GeneKnockdown", paste0("p_", variableID), variableID))
}

### calculate posterior distribution
## load model
load(pgm_file)

## run query
# prepare evidence
if(str_detect(model_id, "DiscreteGeneExpression")){
  ## encode input
  input <- encode_input(input, vmap)
  input[input$variableID == "gene knockdown", "variableID"] <- "gene_knockdown"
  input[input$variableID == "compound treatment", "variableID"] <- "compound_treatment"
}

meas_obs <- list()
if(nrow(input) > 0){
  for(i in 1:nrow(input)){
    meas_obs[[input$variableID[i]]] <- input$variableValue[i]
  }
}

# set data
if(model_id %in% c("SingleGeneKnockdownAndDiscreteGeneExpression", "SingleGeneKnockdownToDiscreteGeneExpression", "DiscreteGeneExpressionToSingleGeneKnockdown")){
  data <- c(list(meas = meas, p_gene_knockdown = p_gene_knockdown), meas_obs)
} else if (model_id %in% c("SingleCompoundTreatmentAndDiscreteGeneExpression", "SingleCompoundTreatmentToDiscreteGeneExpression", "DiscreteGeneExpressionToSingleCompoundTreatment")){
  data <- c(list(meas = meas, p_compound_treatment = p_compound_treatment), meas_obs)
} else {
  data <- meas_obs
}


# set model
model <- jags.model(textConnection(modelstring), data=data)

# run model
update(model,n.iter=100)

# get results
query_dist <- coda.samples(model=model,variable.names=output_spec$variableID, n.iter=10000, thin=1)


## process query results
if(model_id %in% c("SingleGeneKnockdownAndDiscreteGeneExpression", "SingleGeneKnockdownToDiscreteGeneExpression", "DiscreteGeneExpressionToSingleGeneKnockdown")){
  output <- process_query_results(query_dist, output_spec, vmap, discrete = T)
}else if (model_id %in% c("GeneKnockdownAndDiscreteGeneExpression", "GeneKnockdownToDiscreteGeneExpression", "DiscreteGeneExpressionToGeneKnockdown")){
  output <- process_query_results(query_dist, output_spec, discrete = T)
} else {
  output <- process_query_results(query_dist, output_spec)
}


## write JSON
write(prettify(rjson::toJSON(output)), file = output_file)
