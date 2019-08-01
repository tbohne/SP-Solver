library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../res/solutions/solutions_imp.csv", header = TRUE, sep = ",")

solverEntries <- subset(input, solver == "2Cap + TS" | solver == "3Cap + TS" | solver == "OPT" | solver == "2Cap" | solver == "3Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = as.numeric(as.character(time)), y = instance, color = solver, group = solver))
plotPointsPreScaled <- plotPointsPre + geom_point() + xlab("runtime (s)") + ylab("instance")
finalPlot <- plotPointsPreScaled + scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))

ggsave(finalPlot, file="solver_instance_time.png", width = 6, height = 4)

TSData <- subset(input, solver == "2Cap + TS" | solver == "3Cap + TS")
TSRuntime <- subset(TSData, select = c(time))
paste("avg runtime of TS: ", mean(as.numeric(as.character(TSRuntime[["time"]]))))
