# %%
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

print("Reading data")
df = pd.read_csv("results.csv", index_col=False, header=None)
df.columns = [
    "Number of Clients",
    "Total Requests",
    "Total Aggregated Time",
    "Roundtrip",
    "Latency",
    "Throughput",
]
print("Plotting results")
print(df)
one_locations = np.where((df[df.columns[0]] == 1) == True)[0]
dfs = []
for i in range(len(one_locations) - 1):
    dfs.append((df.iloc[one_locations[i] : one_locations[i + 1]]).reset_index())
dfs.append((df.iloc[one_locations[-1] :]).reset_index())
for i, subdf in enumerate(dfs):
    subdf.columns = [col + f"_{i}" for col in subdf.columns]
metrics = pd.concat(dfs, join="outer", axis=1)
metrics.plot(
    x="Number of Clients_1",
    y=[s for s in metrics.columns if "Latency" in s],
    kind="line",
    linestyle="-",
    logy=True,
)
metrics.plot(
    x="Number of Clients_1", y=[s for s in metrics.columns if "Throughput" in s], kind="line", linestyle="-", logy=True
)
plt.show()

# %%
