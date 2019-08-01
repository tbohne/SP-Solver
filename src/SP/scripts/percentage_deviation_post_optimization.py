import sys
import os

def get_instance_idx(line):
    return line.split(",")[0].split("_")[3]

def get_value(line):
    return float(line.split(",")[3].strip())

def get_percentage_deviation(otimal_val, curr_val):
    diff = abs(optimal_val - curr_val)
    return round((diff / (optimal_val) * 100), 2)

def get_runtime(line):
    return float(line.split(",")[2])

def read_optimal_costs():
    costs_file = open("optimal_costs.txt", "r")
    lines = costs_file.readlines()
    costs_file.close()

    optimal_costs = []
    for line in lines:
        if not "time limit" in line:
            optimal_costs.append(float(line.strip()))
    return optimal_costs

def get_time_limit():
    for filename in os.listdir("../../../res/solutions/"):
        if "00_imp.txt" in filename:
            instance_file = open("../../../res/solutions/" + filename, "r")
            break

    lines = instance_file.readlines()
    for line in lines:
        if "time limit:" in line:
            time_limit = float(line.split(" ")[2].strip())
            break
    instance_file.close()
    return time_limit

def get_avg_percentage_deviation(sum_of_deviations, number_of_solutions):
    if number_of_solutions > 0:
        return round((sum_of_deviations / number_of_solutions), 2)
    return None

if __name__ == '__main__':

    solutions_file = open("../../../res/solutions/solutions_imp.csv", "r")
    lines = solutions_file.readlines()
    optimal_costs = read_optimal_costs()
    time_limit = get_time_limit()
    stack_capacity = int(lines[1].split("_")[2])

    optimally_solved = 0

    for line in lines:

        if "TS," in line:
            if optimal_costs[int(get_instance_idx(line))] == get_value(line):
                optimally_solved += 1

    sum_of_deviations_TS = 0.0
    TS_solutions = 0

    for line in lines:

        if "TS," in line:
            instance_idx = get_instance_idx(line)
            curr_val = get_value(line)
            optimal_val = optimal_costs[int(instance_idx)]
            percentage_deviation = get_percentage_deviation(float(optimal_val), curr_val)

            sum_of_deviations_TS += percentage_deviation
            TS_solutions += 1

    num_of_instances = int(instance_idx) + 1
    print("avg percentage deviation TS: " + str(get_avg_percentage_deviation(sum_of_deviations_TS, TS_solutions)))
    print()
    print("optimally solved by TS: " + str((optimally_solved / num_of_instances) * 100) + " %")
