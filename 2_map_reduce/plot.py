# %%
import matplotlib.pyplot as plt
import pandas as pd
import glob

# Get list of all matching CSV files
csv_files = glob.glob("*.csv")

print("Reading data")
eth_list = [(f, pd.read_csv(f, index_col=False)) for f in csv_files]

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
