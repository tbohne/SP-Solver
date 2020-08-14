library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../res/solutions/solutions.csv", header = TRUE, sep = ",")

solverEntries <- subset(input, solver == "GH + TS" | solver == "GH + HC" | solver == "BinP")
plotPointsPre <- ggplot(data = solverEntries, aes(x = as.numeric(as.character(time)), y = instance, color = solver, group = solver))
labeledPlotPoints <- plotPointsPre + geom_point() + xlab("runtime (s)") + ylab("instance")
finalPlot <- labeledPlotPoints #+ scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))
ggsave(finalPlot, file = "runtimes.png", width = 6, height = 4)

compute_avg_runtime <- function(s) {
    data <- subset(input, solver == s)
    runtime <- subset(data, select = c(time))
    return(round(mean(as.numeric(as.character(runtime[["time"]]))), digits = 2))
}

paste("avg runtime of GH + TS: ", compute_avg_runtime("GH + TS"))
paste("avg runtime of GH + HC: ", compute_avg_runtime("GH + HC"))
paste("avg runtime of BinP: ", compute_avg_runtime("BinP"))
paste("avg runtime of 3Idx: ", compute_avg_runtime("3Idx"))
paste("avg runtime of GH: ", compute_avg_runtime("GH"))
