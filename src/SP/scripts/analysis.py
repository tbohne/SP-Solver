import argparse
import sys
import numpy as np
import statistics
from pathlib import Path

INSTANCES_PER_EXPERIMENT = 20

def compute_perc_err(new, initial):
    return abs(new - initial) / initial * 100

def set_avg_costs(costs):
    status.avg_costs_HC = round(statistics.mean(costs["GH + HC"]), 2)
    if len(costs["BinP"]) > 0:
        status.avg_costs_BinP = round(statistics.mean(costs["BinP"]), 2)
    else:
        status.avg_costs_BinP = "---"
    if len(costs["3Idx"]) > 0:
        status.avg_costs_3Idx = round(statistics.mean(costs["3Idx"]), 2)
    else:
        status.avg_costs_3Idx = "---"

def set_avg_runtime(runtimes):
    status.avg_runtime_HC = round(statistics.mean(runtimes["GH + HC"]), 2)
    if len(runtimes["BinP"]) > 0:
        status.avg_runtime_BinP = round(statistics.mean(runtimes["BinP"]), 2)
    else:
        status.avg_runtime_BinP = "---"
    if len(runtimes["3Idx"]) > 0:
        status.avg_runtime_3Idx = round(statistics.mean(runtimes["3Idx"]), 2)
    else:
        status.avg_runtime_3Idx = "---"

def set_optimal_results(costs, best_costs_by_idx, i, optimal, optimally_solved, optimal_runtimes, runtimes):
    if len(costs["BinP"]) > i and costs["BinP"][i] == best_costs_by_idx[i] and optimal[i]:
        optimally_solved["BinP"] += 1
        optimal_runtimes["BinP"].append(runtimes["BinP"][i])
    if len(costs["3Idx"]) > i and costs["3Idx"][i] == best_costs_by_idx[i] and optimal[i]:
        optimally_solved["3Idx"] += 1
        optimal_runtimes["3Idx"].append(runtimes["3Idx"][i])
    if costs["GH + HC"][i] == best_costs_by_idx[i] and optimal[i]:
        optimally_solved["GH + HC"] += 1
        optimal_runtimes["GH + HC"].append(runtimes["GH + HC"][i])

def cnt_best_known(costs, best_costs_by_idx, i, found_best_known):
    if len(costs["BinP"]) > i and costs["BinP"][i] == best_costs_by_idx[i]:
        found_best_known["BinP"] += 1
    if len(costs["3Idx"]) > i and costs["3Idx"][i] == best_costs_by_idx[i]:
        found_best_known["3Idx"] += 1
    if costs["GH + HC"][i] == best_costs_by_idx[i]:
        found_best_known["GH + HC"] += 1

def add_perc_errors(costs, perc_errors, i, best_costs_by_idx):
    if len(costs["BinP"]) > i:
        perc_errors["BinP"].append(compute_perc_err(costs["BinP"][i], best_costs_by_idx[i]))
    if len(costs["3Idx"]) > i:
        perc_errors["3Idx"].append(compute_perc_err(costs["3Idx"][i], best_costs_by_idx[i]))
    if len(costs["GH + HC"]) > i:
        perc_errors["GH + HC"].append(compute_perc_err(costs["GH + HC"][i], best_costs_by_idx[i]))

def set_avg_optimal_runtimes(optimal_runtimes):
    if len(optimal_runtimes["BinP"]) > 0:
        status.avg_optimal_runtime_BinP = round(statistics.mean(optimal_runtimes["BinP"]), 2)
    else:
        status.avg_optimal_runtime_BinP = "---"

    if len(optimal_runtimes["3Idx"]) > 0:
        status.avg_optimal_runtime_3Idx = round(statistics.mean(optimal_runtimes["3Idx"]), 2)
    else:
        status.avg_optimal_runtime_3Idx = "---"

    if len(optimal_runtimes["GH + HC"]) > 0:
        status.avg_optimal_runtime_HC = round(statistics.mean(optimal_runtimes["GH + HC"]), 2)
    else:
        status.avg_optimal_runtime_HC = "---"

def set_avg_perc_dev_from_best_known(perc_errors):
    if len(perc_errors["BinP"]) > 0:
        status.avg_perc_dev_from_best_known_BinP = round(statistics.mean(perc_errors["BinP"]), 6)
    else:
        status.avg_perc_dev_from_best_known_BinP = "---"

    if len(perc_errors["3Idx"]) > 0:
        status.avg_perc_dev_from_best_known_3Idx = round(statistics.mean(perc_errors["3Idx"]), 6)
    else:
        status.avg_perc_dev_from_best_known_3Idx = "---"

    if len(perc_errors["GH + HC"]) > 0:
        status.avg_perc_dev_from_best_known_HC = round(statistics.mean(perc_errors["GH + HC"]), 6)
    else:
        status.avg_perc_dev_from_best_known_HC = "---"

def set_perc_opt(optimal_solutions_known, optimally_solved):
    if optimal_solutions_known:
        status.perc_opt_3Idx = optimally_solved["3Idx"] / INSTANCES_PER_EXPERIMENT * 100
        status.perc_opt_BinP = optimally_solved["BinP"] / INSTANCES_PER_EXPERIMENT * 100
        status.perc_opt_HC = optimally_solved["GH + HC"] / INSTANCES_PER_EXPERIMENT * 100
    else:
        status.perc_opt_3Idx = "unk."
        status.perc_opt_BinP = "unk."
        status.perc_opt_HC = "unk."

def set_best_known(found_best_known):
    status.best_known_BinP = found_best_known["BinP"]
    status.best_known_3Idx = found_best_known["3Idx"]
    status.best_known_HC = found_best_known["GH + HC"]

def create_entries_for_file(filename, status):
    costs = {}
    costs["BinP"] = []
    costs["3Idx"] = []
    costs["GH + HC"] = []

    runtimes = {}
    runtimes["BinP"] = []
    runtimes["3Idx"] = []
    runtimes["GH + HC"] = []

    runtimes_to_best = []
    iterations_to_best = []
    total_iterations = []
    initial_values = []
    lower_bounds = []

    with open(filename, "r") as f:

        lines = f.readlines()[1:]
        status.instance_type = "_".join(lines[1].split(",")[0].split("_")[:3])
        status.time_limit = float(lines[1].split(",")[2])
        idx = 0
        best_costs = sys.maxsize
        best_costs_by_idx = []
        optimal = [False for i in range(INSTANCES_PER_EXPERIMENT)]

        for line in lines:

            tmp_idx = int(line.strip().split(",")[0].split("_")[-1])

            if tmp_idx != idx:
                best_costs_by_idx.append(best_costs)
                best_costs = sys.maxsize
                idx = tmp_idx

            if "BinP" in line:
                tmp_costs = float(line.strip().split(",")[4])
                costs["BinP"].append(tmp_costs)
                if tmp_costs < best_costs:
                    best_costs = tmp_costs
                tmp_runtime = float(line.strip().split(",")[3])
                runtimes["BinP"].append(tmp_runtime)
                if tmp_runtime < status.time_limit:
                    optimal[idx] = True

            elif "3Idx" in line:
                tmp_costs = float(line.strip().split(",")[4])
                costs["3Idx"].append(tmp_costs)
                if tmp_costs < best_costs:
                    best_costs = tmp_costs
                tmp_runtime = float(line.strip().split(",")[3])
                runtimes["3Idx"].append(tmp_runtime)
                if tmp_runtime < status.time_limit:
                    optimal[idx] = True

            elif "GH + HC" in line:
                tmp_costs = float(line.strip().split(",")[4])
                costs["GH + HC"].append(tmp_costs)
                if tmp_costs < best_costs:
                    best_costs = tmp_costs
                iterations_to_best.append(int(line.strip().split(",")[8]))
                runtimes["GH + HC"].append(float(line.strip().split(",")[3]))
                runtimes_to_best.append(float(line.strip().split(",")[7]))
                total_iterations.append(int(line.strip().split(",")[9]))

            elif "GH" in line and not "GH + TS" in line:
                initial_values.append(float(line.split(",")[4]))

            elif "LB" in line:
                lower_bounds.append(float(line.split(",")[4]))

        best_costs_by_idx.append(best_costs)
        set_avg_costs(costs)

        avg_initial = statistics.mean(initial_values)
        percentage_improvement = compute_perc_err(status.avg_costs_HC, avg_initial)
        status.avg_perc_imp = round(percentage_improvement, 2)
        status.avg_iter_to_best = statistics.mean(iterations_to_best)
        set_avg_runtime(runtimes)

        status.avg_time_to_best = round(statistics.mean(runtimes_to_best), 2)
        status.avg_iterations = statistics.mean(total_iterations)
        status.feasible_BinP = len(costs["BinP"])
        status.feasible_3Idx = len(costs["3Idx"])

        perc_errors = {}
        perc_errors["BinP"] = []
        perc_errors["3Idx"] = []
        perc_errors["GH + HC"] = []

        optimally_solved = {}
        optimally_solved["BinP"] = 0
        optimally_solved["3Idx"] = 0
        optimally_solved["GH + HC"] = 0

        found_best_known = {}
        found_best_known["BinP"] = 0
        found_best_known["3Idx"] = 0
        found_best_known["GH + HC"] = 0

        optimal_runtimes = {}
        optimal_runtimes["BinP"] = []
        optimal_runtimes["3Idx"] = []
        optimal_runtimes["GH + HC"] = []

        for i in range(0, len(best_costs_by_idx)):
            set_optimal_results(costs, best_costs_by_idx, i, optimal, optimally_solved, optimal_runtimes, runtimes)
            cnt_best_known(costs, best_costs_by_idx, i, found_best_known)
            add_perc_errors(costs, perc_errors, i, best_costs_by_idx)

        set_avg_optimal_runtimes(optimal_runtimes)
        set_avg_perc_dev_from_best_known(perc_errors)

        avg_best_costs = statistics.mean(best_costs_by_idx)
        avg_lower_bound = statistics.mean(lower_bounds)
        status.avg_perc_dev_from_LB_best_known = round(compute_perc_err(avg_best_costs, avg_lower_bound), 6)
        optimal_solutions_known = all(p == True for p in optimal) and len(optimal) > 0

        set_perc_opt(optimal_solutions_known, optimally_solved)
        set_best_known(found_best_known)

        status.write_to_csv(args['output'])

class SolutionStatus:

    def __init__(self):
        self.instance_type = ""
        self.time_limit = 0
        self.feasible_BinP = 0
        self.feasible_3Idx = 0
        self.best_known_BinP = 0
        self.best_known_3Idx = 0
        self.best_known_HC = 0
        self.perc_opt_3Idx = 0
        self.perc_opt_BinP = 0
        self.perc_opt_HC = 0
        self.avg_costs_BinP = 0
        self.avg_costs_3Idx = 0
        self.avg_costs_HC = 0
        self.avg_runtime_BinP = 0
        self.avg_runtime_3Idx = 0
        self.avg_runtime_HC = 0
        self.avg_perc_dev_from_best_known_BinP = 0
        self.avg_perc_dev_from_best_known_3Idx = 0
        self.avg_perc_dev_from_best_known_HC = 0
        self.avg_perc_dev_from_LB_best_known = 0
        self.avg_optimal_runtime_BinP = 0
        self.avg_optimal_runtime_3Idx = 0
        self.avg_optimal_runtime_HC = 0
        self.avg_perc_imp = 0
        self.avg_iter_to_best = 0
        self.avg_time_to_best = 0
        self.avg_iterations = 0

    def setup_csv(self, filename):
        with open(filename, "w") as f:
            heads = [attr for attr, _ in self.__iter__()]
            for h in heads:
                f.write(h + ",")
            f.write("\n")

    def __iter__(self):
        for attr, value in self.__dict__.items():
            yield attr, value

    def write_to_csv(self, filename):
        with open(filename, "a") as f:
            data = [val for _, val in self.__iter__()]
            for d in data:
                f.write(str(d) + ",")
            f.write("\n")

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='comparing SP solvers')
    parser.add_argument('-i', '--input', help = 'input directory', required = True)
    parser.add_argument('-o', '--output', help = 'output file', required = True)
    args = vars(parser.parse_args())

    pathlist = Path(args['input']).glob('**/*.csv')
    # only working for /final_exp directory
    pathlist = sorted(pathlist, key=lambda path: (int(str(path).split("_")[2][2]), int(str(path).split("_")[3].split(".")[0])))

    status = SolutionStatus()
    status.setup_csv(args["output"])

    for path in pathlist:
        filename = str(path).strip()
        print("working on:", filename)
        status = SolutionStatus()
        create_entries_for_file(filename, status)
