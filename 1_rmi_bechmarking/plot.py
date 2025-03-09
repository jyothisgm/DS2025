# %%
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

print("Reading data")
df = pd.read_csv("results.csv", index_col=False)
# df.columns = [
#     "Number of Clients",
#     "Total Requests",
#     "Total Aggregated Time",
#     "Roundtrip",
#     "Latency",
#     "Throughput",
# ]
print("Plotting results")
print(df)
# %%
one_locations = np.where((df[df.columns[0]] == 1) == True)[0]
step = len(one_locations)
dfs = []
for offset in range(len(one_locations)):
    dfs.append((df.iloc[offset::step]).reset_index())
for i, subdf in enumerate(dfs):
    subdf.columns = [col + f"_{i+1}" for col in subdf.columns]
metrics = pd.concat(dfs, join="outer", axis=1)

# %%
fig, ax = plt.subplots()
metrics.plot(
    x="NClients_1",
    xlabel="Number of Clients",
    ylabel="Latency (μs)",
    y=[s for s in metrics.columns if "Latency" in s],
    title="Latency",
    kind="line",
    linestyle="-",
    logy=True,
    ax=ax,
)
ax.legend(["Sequence", "Array", "Complex Object"])
plt.savefig("results_latency.png")
fig, ax = plt.subplots()
metrics.plot(
    x="NClients_1",
    xlabel="Number of Clients",
    ylabel="Throughput (bps)",
    y=[s for s in metrics.columns if "Throughput" in s],
    title="Throughput",
    kind="line",
    linestyle="-",
    logy=True,
    ax=ax,
)
ax.legend(["Sequence", "Array", "Complex Object"])
plt.savefig("results_throughput.png")
# plt.show()

# %%
