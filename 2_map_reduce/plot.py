# %%
import matplotlib.pyplot as plt
import pandas as pd
import glob
import numpy as np
from itertools import cycle


# Get list of all matching CSV files
csv_files = glob.glob("*.csv")

print("Reading data")
eth_list = [(f, pd.read_csv(f, index_col=False)) for f in csv_files]

max_val = 0
for _, each_metric in eth_list:
    # Without Post Processing
    each_metric['TotalTimeWOPP'] = each_metric['MapTime'] + each_metric['ReduceTime']
    temp_max_time = each_metric['TotalTimeWOPP'].max()
    if max_val < temp_max_time:
        max_val = temp_max_time
for _, each_metric in eth_list:
    each_metric['TotalTimeWOPPNorm'] = max_val/each_metric['TotalTimeWOPP']


print("Plotting results")

# %%
# Plot general comparison
plt.figure(figsize=(7, 6))

# Define colors or markers if needed

metrics = ['MapTime', 'ReduceTime','TotalTimeWOPP']
# metrics = ['MapTime', 'ReduceTime', 'PostProcessingTime', 'TotalTime']
yticks = [1000, 2000, 4000, 8000, 16000, 32000, 48000, 64000, 96000]

for filename, df in eth_list:
    color_cycle = cycle(plt.rcParams['axes.prop_cycle'].by_key()['color'])
    if "Combiner" in filename or "ib" in filename:
        continue
    for metric in metrics:
        color = next(color_cycle)
        label = f"{metric} - {filename.split(".")[0]}".replace("WOPP","")
        linetype = '-'
        if "InvertedIndex" in filename:
            linetype = "--"
        plt.plot(df['NClients'], df[metric], marker='.',color = color,linestyle=linetype, label=label)


# Apply log scale and custom ticks
plt.yscale('log')
plt.yticks(yticks, [f"{int(y/1000)}" for y in yticks])
plt.xticks(range(1,16))
plt.xlabel('Number of Clients')
plt.ylabel('Time (s)')
plt.title('Time comparison vs Number of Clients for Wordcount, InvertedIndex')

plt.grid(True, which='both', linestyle='--', linewidth=0.5)
plt.legend()
# plt.tight_layout()
plt.show()

# %%
# Plot combiner comparison
plt.figure(figsize=(7, 6))

# Define colors or markers if needed

metrics = ['MapTime', 'ReduceTime','TotalTimeWOPP']
# metrics = ['MapTime', 'ReduceTime', 'PostProcessingTime', 'TotalTime']
yticks = [1000, 2000, 4000, 8000, 16000, 32000, 48000, 64000]

for filename, df in eth_list:
    color_cycle = cycle(plt.rcParams['axes.prop_cycle'].by_key()['color'])
    if "InvertedIndex" in filename or "ib" in filename:
        continue
    for metric in metrics:
        color = next(color_cycle)
        label = f"{metric} - {filename.split(".")[0]}".replace("WOPP","")
        linetype = '-'
        if "Combiner" in filename:
            linetype = "--"
        plt.plot(df['NClients'], df[metric], marker='.',color = color,linestyle=linetype, label=label)


# Apply log scale and custom ticks
plt.yscale('log')
plt.yticks(yticks, [f"{int(y/1000)}" for y in yticks])
plt.xticks(range(1,16))
plt.xlabel('Number of Clients')
plt.ylabel('Time (s)')
plt.title('Time comparison with/without WordCount combiner function')

plt.grid(True, which='both', linestyle='--', linewidth=0.5)
plt.legend()
# plt.tight_layout()
plt.show()
# %%
plt.figure(figsize=(7, 6))

# Define colors or markers if needed

metrics = ['TotalTimeWOPPNorm']
# yticks = [1000, 2000, 4000, 8000, 16000, 32000, 48000, 64000, 90000]

color_cycle = cycle(plt.rcParams['axes.prop_cycle'].by_key()['color'])
for filename, df in eth_list:
    if "ib" in filename or "ib" in filename:
        continue
    for metric in metrics:
        color = next(color_cycle)
        label = f"{filename.split(".")[0]}"
        print(df[metric].min())
        linetype = '-'
        if "Combiner" in metric:
            linetype = "--"
        plt.plot(np.linspace(1, 15, 100), np.linspace(1, 15, 100)*df[metric].min(), linestyle='--', color=color)
        plt.plot(df['NClients'], df[metric], marker='o', label=label, color=color)


# Apply log scale and custom ticks
# plt.yscale('log')
# plt.yticks(yticks, [f"{int(y/1000)}" for y in yticks])
plt.xlabel('Number of Clients')
plt.ylabel('Time (s)')
plt.xticks(range(1,16))
plt.title('Speedup vs Number of Clients')

plt.grid(True, which='both', linestyle='--', linewidth=0.5)
plt.legend()
plt.tight_layout()
plt.show()
# %%
