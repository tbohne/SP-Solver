library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "3Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = as.numeric(as.character(time)), y = instance, color = solver, group = solver)) + geom_point() + xlab("runtime (s)") + ylab("instance")

####################################################################################################
binpData <- subset(input, solver == "BinP")
binpRuntime <- subset(binpData, select = c(time))
paste("avg runtime of BinP: ", mean(as.numeric(as.character(binpRuntime[["time"]]))))

threeidxData <- subset(input, solver == "3Idx")
threeidxRuntime <- subset(threeidxData, select = c(time))
paste("avg runtime of 3Idx: ", mean(as.numeric(as.character(threeidxRuntime[["time"]]))))

threeCapData <- subset(input, solver == "3Cap")
threeCapRuntime <- subset(threeCapData, select = c(time))
paste("avg runtime of 3Cap: ", mean(as.numeric(as.character(threeCapRuntime[["time"]]))))
####################################################################################################

finalPlot <- plotPointsPre + scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))
ggsave(finalPlot, file="solver_instance_time.png", width=6, height=4)
