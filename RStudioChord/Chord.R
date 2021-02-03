# Title     : Chord Visualizer
# Objective : Map the same items as the main program is doing
# Created by: david
# Created on: 11/17/2020
# R Version : R-4.0.3 : win

install.packages("circlize")
install.packages("digest")
install.packages("strtoi")
library(circlize)
library(digest)
library(strtoi)

# setwd("~/Desktop/")
set.seed(999)

data <- read.csv(file = 'pruned_data.csv')
clean <- data[which(!grepl("[^\x01-\x7F]+", data$original_title)),]
clean <- clean[clean$revenue > 100, ]
clean <- clean[2:4]
clean2 = clean

#Create SHA-1 Hash for all entries
clean2$original_title <- lapply(clean2$original_title, digest, algo = "sha1")
clean2$original_title <- strtrim(clean2$original_title, 5)
chordDiagram(clean2)

clean3 = clean2
clean3$original_title <- strtoi(clean2$original_title, 0L)
clean3$original_title <- strtrim(clean2$original_title, 2)
chordDiagram(clean3)

clean4 = clean3
clean4$release_date <- strtrim(clean2$release_date, 4)
chordDiagram(clean4)

clean5 = clean4
clean5$release_date <- strtrim(clean2$original_title, 5)
chordDiagram(clean5)

# by(clean, 1:nrow(clean), function(row) print(row))

line = clean[2:4,0]

chordDiagram(clean)
sapply("MovieName", digest, algo = "sha1")
sapply("MovieName", sha1 , digits = 5)
x = sapply("MovieName", sha1 , digits = 5)


#Example without data
mat = matrix(sample(100, 100), 3, 6)
names = c("Title","Date","Revenue")
df = data.frame(from = rep(rownames(mat), times = ncol(mat)),
                to = rep(colnames(mat), each = nrow(mat)),
                value = as.vector(mat),
                stringsAsFactors = TRUE)
rownames(mat) = paste0("LABEL" , 1:3)
colnames(mat) = paste0("DATA", 1:6)
chordDiagram(mat)

