library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")

solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "2Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = as.numeric(as.character(time)), y = instance, color = solver, group = solver))
plotPointsPreScaled <- plotPointsPre + geom_point() + xlab("runtime (s)") + ylab("instance")
finalPlot <- plotPointsPreScaled + scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))

ggsave(finalPlot, file="solver_instance_time.png", width = 6, height = 4)

####################################################################################################
binpData <- subset(input, solver == "BinP")
binpRuntime <- subset(binpData, select = c(time))
paste("avg runtime of BinP: ", mean(as.numeric(as.character(binpRuntime[["time"]]))))

threeidxData <- subset(input, solver == "3Idx")
threeidxRuntime <- subset(threeidxData, select = c(time))
paste("avg runtime of 3Idx: ", mean(as.numeric(as.character(threeidxRuntime[["time"]]))))

twoCapData <- subset(input, solver == "2Cap")
twoCapRuntime <- subset(twoCapData, select = c(time))
paste("avg runtime of 2Cap: ", mean(as.numeric(as.character(twoCapRuntime[["time"]]))))
####################################################################################################
