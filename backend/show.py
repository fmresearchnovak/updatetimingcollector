#!/usr/bin/env python3

import sys


def main():

	fh = open(sys.argv[1], 'r')

	fh.readline()
	fh.readline()
	headings = fh.readline().split(",")
	print(headings)

main()