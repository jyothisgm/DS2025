# %%
import matplotlib.pyplot as plt
import pandas as pd

print("Reading data")
eth = pd.read_csv("results.csv", index_col=False)
ib = pd.read_csv("results_ib.csv", index_col=False)

# df.columns = [
#     "Number of Clients",
#     "Total Requests",
#     "Total Aggregated Time",
#     "Roundtrip",
#     "Latency",
#     "Throughput",
# ]
print("Plotting results")
# %%
ib_text = " (infiniband)"
eth_dfs = {obj_type: sub_df for obj_type, sub_df in eth.groupby("Type") if obj_type != "Type"}
ib_dfs = {obj_type+ib_text: sub_df for obj_type, sub_df in ib.groupby("Type") if obj_type != "Type"}
dfs = {**eth_dfs,**ib_dfs}
fig, ax = plt.subplots()

labels = ["Array", "Sequence"]
labels += [i+ ib_text for i in labels]
# labels = ["Sequence_NoSync", "Sequence"]
#%%
for label, each_df in dfs.items():
    new_df = each_df.set_index('NClients')
    new_df.index.name = None
    if label in labels:
        plt.plot(each_df['NClients'].astype(int), each_df.reset_index(drop=True).set_index('NClients')['Latency'].astype(float), marker='o', label=label)
        ax.set_xticks(each_df['NClients'].astype(int))
plt.xlabel("Number of Clients")
plt.ylabel("Latency (μs)")
plt.yscale("log")
plt.title("Latency")
plt.legend()
plt.show()

fig, ax = plt.subplots()

for label, each_df in dfs.items():
    new_df = each_df.set_index('NClients')
    new_df.index.name = None
    if label in labels:
        plt.plot(each_df['NClients'].astype(int), each_df.reset_index(drop=True).set_index('NClients')['Throughput'].astype(float), marker='o', label=label.replace("_", " "))
        ax.set_xticks(each_df['NClients'].astype(int))
plt.xlabel("Number of Clients")
plt.ylabel("Throughput (bps)")
plt.title("Throughput")
plt.yscale("log")
plt.legend()
plt.show()

# %%
