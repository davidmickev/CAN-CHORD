# Movie data parser to create key(movie) -> value(release_date, gross_income)

# import packages
import numpy as np
import pandas as pd
import sys
import json
import math
import time

# * Assume default name and replace if name is provided
data_filename = "movies_metadata.csv"
if len(sys.argv) > 1:
    data_filename = sys.argv[1]

df: pd.DataFrame = None
# * Catch Exceptions while loading the file
try:
    # * Get the data from the csv file
    df: pd.DataFrame = pd.read_csv(data_filename,nrows=250,usecols=['original_title','release_date','revenue'],low_memory = False, encoding='utf8')
    # same as using #df.drop(df.columns.difference(['original_title','release_date','revenue']), 1, inplace=True)
    
    # * drop all data that has 0 as revenue
    df = df[df.revenue != 0]
    # * show data summarry 
    #print(df.info)

except IOError as err:
    print(err)
    exit(1)

# * Limit to first 100 in output csv if csv
#df.head(100).to_csv("pruned_data.txt")

fout = open("application.conf","w")
fout.write("dictionary {\n")

for ind in df.index: 
    #fout.write(df['original_title'][ind], df['release_date'][ind], df['revenue'][ind])
    #line = (df['original_title'][ind], df['release_date'][ind], df['revenue'][ind])
    if ind == 28:
        continue
    print(ind)
    try:
        title = (df['original_title'][ind])
        title = title.replace(" ", "").replace("'", "").replace(":", "").replace(",", "").replace("!", "") 
        line = str("\t" + title + " = " + "\"" 
        + str(df['release_date'][ind]) + " $" +str(df['revenue'][ind])+ "\"\n")
        fout.write(line)
    except:
        continue

fout.write("\n}")
fout.close()