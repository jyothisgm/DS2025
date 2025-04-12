# %%
import matplotlib.pyplot as plt
import pandas as pd

print("Reading data")
eth = pd.read_csv("results.csv", index_col=False)
# ib = pd.read_csv("results_ib.csv", index_col=False)

print("Plotting results")
# %%
# Plot
ax = eth.set_index('NClients')[['MapTime', 'ReduceTime', 'PostProcessingTime', 'TotalTime']].plot(
    figsize=(10, 6),
    marker='o',
    logy=True
)

# Set custom y-ticks so values are visible
yticks = [1000, 2000, 4000, 8000, 16000, 32000, 40000]
ax.set_yticks(yticks)
ax.set_yticklabels([str(y) for y in yticks])

# Labels and title
plt.title('Time Metrics vs Number of Clients (Log Scale)')
plt.xlabel('Number of Clients')
plt.ylabel('Time (ms)')
plt.grid(True, which="both", linestyle="--", linewidth=0.5)
plt.legend(title="Metric")
plt.tight_layout()
plt.show()
# %%