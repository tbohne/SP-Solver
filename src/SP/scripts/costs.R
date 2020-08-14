library(ggplot2)

input <- read.csv(file = "../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "GH + TS" | solver == "GH + HC" | solver == "BinP")
plotPointsPre <- ggplot(data = solverEntries, aes(x = val, y = instance, color = solver, group = solver))
scaledPlot <- plotPointsPre + geom_point() + xlab("costs") + ylab("instance")
finalPlot <- scaledPlot #+ scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))
ggsave(finalPlot, file = "solver_instance_cost.png", width=6, height=4)

compute_avg_costs <- function(s) {
    data <- subset(input, solver == s)
    costs <- subset(data, select = c(val))
    return(round(mean(as.numeric(as.character(costs[["val"]]))), digits = 2))
}

paste("avg costs of GH + TS: ", compute_avg_costs("GH + TS"))
paste("avg costs of GH + HC: ", compute_avg_costs("GH + HC"))
paste("avg costs of BinP: ", compute_avg_costs("BinP"))
paste("avg costs of 3Idx: ", compute_avg_costs("3Idx"))
paste("avg costs of GH: ", compute_avg_costs("GH"))
