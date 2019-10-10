library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions_imp.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "2Cap + TS" | solver == "3Cap + TS" | solver == "OPT" | solver == "2Cap" | solver == "3Cap")

plotPointsPre <- ggplot(data = solverEntries, aes(x = val, y = instance, color = solver, group = solver))
scaledPlot <- plotPointsPre + geom_point() + xlab("costs") + ylab("instance")
finalPlot <- scaledPlot + scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))

ggsave(finalPlot, file = "solver_instance_cost.png", width=6, height=4)
