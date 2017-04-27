#
#  R script for a mock model
#

args = commandArgs(TRUE)

if (length(args)>=2){ 
  
  inputFile = args[1]
  outputFile = args[2]

  input <- read.table(inputFile,sep="\t", header=TRUE,stringsAsFactors=FALSE)
  
  for (i in 1:length(input$io)){
    if (input$io[i] == "output"){
      input$variableValue[i] = "seedless"
      input$probability[i] = runif(1)
    }
  }
  
  write.table(input, outputFile, sep="\t", row.names=F, quote=F, na="")
  
} else {
  print('R: not enough arguments: <inputFile> <outputFile>')
  quit("no",10, FALSE)
}
