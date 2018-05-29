#!/usr/bin/env python3

import glob
import os
import subprocess
from subprocess import PIPE
import sys
import stat
import moments

# Author: Prof. Novak
# Description: This script uses a companion ruby script
# To scrape the Google Play Store and gather information
# about the apps that we've seen from the Update Timing Collector
# Android application

# It gathers unique names from all .csv files (it assumes to be running
# with that folder in it's path).  Then, it uses the ruby script
# to scrape the info from the play store.  The most important
# piece of info is the "Last Updated Day" field which should give us
# an idea about what day / time the app update became available

# This script is designed to be run once daily (probably using cron)


ALL_777 = stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR | \
stat.S_IRGRP | stat.S_IWGRP | stat.S_IXGRP | \
stat.S_IROTH | stat.S_IWOTH | stat.S_IXOTH







def main():

	# Change to directory given (if any given)
	if len(sys.argv) == 2:
		os.chdir(sys.argv[1])
	else:
		os.chdir("/srv/update-timing-collector-backend/data/")

	# Lookup all of the CSV files in this directory.
	# This is meant to be run in the data/ directory (see line above)
	# on the server where all of the .csv files are
	# stored from the various installations of the app
	# 
	# This loop collects all unique app (package) names
	# Across all of the log files we have.
	names = []
	for file in glob.glob("*.csv"):

		#print(file)
		fh = open(file, 'r')

		# This is a hacky check for the couple
		# files that have the incorrect format.
		if(len(fh.readline()) < 20):
			fh.readline()
			fh.readline()

		# Read this this file and pull out the app names (package names)
		# Collect the unique names found
		for line in fh:
			line = line.strip().split(",")
			print(line)
			name = line[3]
			
			if name not in names: # no repeats
				names.append(name)

		#print()

	# Debugging
	#for n in names:
	#	print(n)
	#print()



	# Open output file and write information for each app
	# The format of each row is as follows
	# packagename,App Name,version no., millisecond timestamp (unix time ms)
	#
	# The file name is the current time in milliseconds (unix time ms)
	# in the availability/ folder which should already exist!!!
	fName = "availability/" + str(moments.currentTimeMillis()) + ".csv"
	print("fName:", fName)
	outF = open(fName, 'w')
	os.chmod(fName, ALL_777)
	#os.chown(fname, 1001, 1001)

	for n in names:
		print("App Name:", n)

		# Use companion ruby script to lookup information about
		# this app.  For many apps (mostly pre-installed system apps)
		# There is no information about it on the play store.
		p = subprocess.Popen(['scrapper', n], stdin=PIPE, stdout=PIPE, stderr=PIPE)
		output, err = p.communicate()
		rc = p.returncode


		output = output.decode("utf-8").strip().split("\n")
		print("output:", output)
		print("err:", err)
		# Debugging
		#print("err == None:", err == None, "  len(err):", len(err))
		#print("rc:", rc)

		# For apps that cannot be found in the play store an error is given
		# In these cases we simply write the app name to the file with no other info
		if(len(err) == 0):
			appInfo = [n] + output
			appInfo[3] = str(moments.dateStringToUnixTS(appInfo[3]))
		else:
			appInfo = [n]

		# See what we're writing, convert then write
		print("app Info: ", appInfo)
		s = ",".join(appInfo) + "\n"
		outF.write(s)
		print("WRITING NOW!!")

		print() # blank line for aesthetics

	outF.close()

main()


