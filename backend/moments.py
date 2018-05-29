#!/usr/bin/env python3

# Author: Prof. Novak
# Desc: Helpful little library with a couple conversion functions

import time



def currentTimeMillis():
	# Get the current time (local to this machine) in ms (unix time in ms)
	# This is used to name the output  ".csv" file
	return int(round(time.time() * 1000))

def msToMinutes(ts):
	# Convert milliseconds to Minutes
	return ts // 60000

def minutesToSeconds(ts):
	# Convert minutes to seconds
	return ts * 60

def msToSeconds(ts):
	return ts / 1000

def secondsToTimeCode(ts):
	h = ts // (60 * 60)
	ts = ts - (h * 60 * 60)

	m = ts // (60)
	ts = ts - (m * 60)

	s = ts

	tc = TimeCode("+", h, m, s)
	return ts


def dateStringToUnixTS(dateString):
	# Convert a date string (Month day, year)
	# to a millisecond timestamp (unix time in ms)
	ans = time.mktime(datetime.datetime.strptime(dateString, "%B %d, %Y").timetuple())
	return ans

def getDateFromTimeStamp(ts, formatStr = "%D %H:%M"):
	# Convert a ms unix timestamp to a date string (defaults to "Day HH:MM" in 24hr time)
	return time.strftime(formatStr, time.localtime(ts))



# timecode.py

# Author: Prof. Novak

# Implements a HH:MM:SS style time code that might be used for example, with a movie / song

class TimeCode:
    def __init__(self, sign, newHour, newMinute, newSecond):
        if(sign not in ["+", "-"]):
            raise ValueError("Invalid sign: " + sign)

        if(newHour < 0):
            raise ValueError("Invalid hour: " + str(newHour))

        if(newMinute < 0 or newMinute > 60):
            raise ValueError("Invalid minute: " + str(newMinute))

        if(newSecond < 0 or newSecond > 60):
            raise ValueError("Invalid second: " + str(newSecond))


        self.__seconds = (newHour * 60 * 60) + (newMinute * 60) + newSecond

        if(sign == "-"):
            self.__seconds = -self.__seconds

        #print("finished constructing.  seconds: ", self.__seconds)


    def setSeconds(self, newS):
    	self.__seconds = newS


    def __repr__(self):
    	return str(self.__seconds)

    def __str__(self):
        if(self.__seconds >= 0):
            sign = "+"
        else:
            sign = "-"

        s = abs(self.__seconds)
        #print("s:", s)

        # Calculate the integer number of hours
        hours = s // (60 * 60)
        s = s - (hours * 60 * 60) # remove the hours

        # Calculate integer number of minutes
        # The hours were already removed, so there 
        # is only minutes + seconds left in s
        minutes = s // (60)
        s = s - (minutes * 60)

        seconds = s
        
        return sign + str(hours) + ":" + str(minutes) + ":" + str(seconds)

    def __add__(self, rhs):
        newS = self.__seconds + rhs.__seconds
        newTime = TimeCode("+", 0, 0, 0)
        newTime.__seconds = newS
        return newTime

    def __neg__(self):
    	newTime = TimeCode("+", 0, 0, 0)
    	newTime.__seconds = -self.__seconds
    	return newTime

    def __sub__(self, rhs):
        res = self + -rhs
        return res

    def __int__(self):
        return self.__seconds
 
    def __getitem__(self, idx):
        s = str(self)
        s = s.split(":")
        ans = s[idx]
        return int(ans)

    def __eq__(self, other):
        return self.__seconds == other.__seconds

    def __lt__(self, other):
        return self.__seconds < other.__seconds

    def __le__(self, other):
        return self.__seconds <= other.__seconds

    def __ge__(self, other):
        return self.__seconds >= other.__seconds

    def __gt__(self, other):
    	return self.__seconds > other.__seconds

    def __ne__(self, other):
    	return not(self == other)








