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
# Plot
plt.figure(figsize=(12, 8))

# Define colors or markers if needed

metrics = ['MapTime', 'ReduceTime', 'PostProcessingTime', 'TotalTime']
yticks = [1000, 2000, 4000, 8000, 16000, 32000, 48000, 64000, 90000]

for filename, df in eth_list:
    for metric in metrics:
        label = f"{metric} - {filename.split(".")[0]}"
        plt.plot(df['NClients'], df[metric], marker='o', label=label)


# Apply log scale and custom ticks
plt.yscale('log')
plt.yticks(yticks, [f"{int(y/1000)}" for y in yticks])
plt.xlabel('Number of Clients')
plt.ylabel('Time (s)')
plt.title('Time Metrics vs Number of Clients (All Files)')

plt.grid(True, which='both', linestyle='--', linewidth=0.5)
plt.legend()
plt.tight_layout()
plt.show()
# %%
plt.figure(figsize=(12, 8))

# Define colors or markers if needed

metrics = ['TotalTimeWOPPNorm']
# yticks = [1000, 2000, 4000, 8000, 16000, 32000, 48000, 64000, 90000]

color_cycle = cycle(plt.rcParams['axes.prop_cycle'].by_key()['color'])
for filename, df in eth_list:
    for metric in metrics:
        color = next(color_cycle)
        label = f"{metric} - {filename.split(".")[0]}"
        print(df[metric].min())

        plt.plot(np.linspace(1, 15, 100), np.linspace(1, 15, 100)*df[metric].min(), linestyle='--', color=color)
        plt.plot(df['NClients'], df[metric], marker='o', label=label, color=color)


# Apply log scale and custom ticks
# plt.yscale('log')
# plt.yticks(yticks, [f"{int(y/1000)}" for y in yticks])
plt.xlabel('Number of Clients')
plt.ylabel('Time (s)')
plt.title('Time Metrics vs Number of Clients (All Files)')

plt.grid(True, which='both', linestyle='--', linewidth=0.5)
plt.legend()
plt.tight_layout()
plt.show()
# %%
