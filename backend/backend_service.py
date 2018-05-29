#!/usr/bin/env python3

# backend_service.py
# Author: Ed Novak
# Description:  This is the backend server.  It should be run on
# some machine with a publically facing IP address and it will 

import time
from http.server import BaseHTTPRequestHandler, HTTPServer
import os
import hashlib
import string

HOST_NAME = '155.68.120.41'
PORT_NUMBER = 9000
LOG_FILE_NAME = "logfile.txt"


def checkName(s):
    if ".." in s or "/" in s or "\\" in s:
        return False

    valid_chars = string.digits + "_new.csv"
    for character in s:
        if character not in valid_chars:
            return False
    return True

def sha256(data):
    H = hashlib.sha256()
    H.update(data.encode())
    return H.digest().hex()


def getFile(fileName):
    path = "./data/"

    # accomodate the newer style logs with Android 8+
    if(fileName[-8:] == "_new.csv"):
    	path = "./data/new/"

    if not os.path.exists(path):
        os.makedirs(path)
    absPath = path + fileName
    return absPath

def writeFile(fileName, c, codeLetter):
    absPath = getFile(fileName)
    fh = open(absPath, codeLetter)
    fh.write(c)
    fh.close()
    return absPath


class FileHandler(BaseHTTPRequestHandler):



    def do_HEAD(self):
        s = str(time.asctime()) + " HEAD!!  at" + str(self.path) + "\n"
        writeFile(LOG_FILE_NAME, s, "a")
        self.respond_ok()


    def do_GET(self):
        s = str(time.asctime()) + " GET!!  at" + str(self.path) + "\n"
        writeFile(LOG_FILE_NAME, s, "a")
        self.respond_ok()


    def do_POST(self):
        logFile = getFile(LOG_FILE_NAME)
        logFH = open(logFile, 'a')

        s = str(time.asctime()) + " POST!!  at:" + str(self.path) + "\n"
        logFH.write(s)

        content_length = int(self.headers['content-length']) # size of data_file
        #post_data = self.rfile.read(content_length)
        post_data = self.rfile.read(content_length).decode("UTF-8")
        #print("posted data:", repr(post_data))

        name = ""
        try:
            name = post_data.split("\r\n")[1].split("=")[2].strip("\"")
            if(not checkName(name)):
                raise IndexError

        except IndexError:
            s = "Invalid file name.  Possibly malicious!  Ignoring file: " + name
            logFH.write(s)
            self.finish_clean(logFH)
            return
        
        #print("\nName: ", name, repr(name))

        file_contents = post_data.split("\r\n")[4].strip("-")
        #print("\nFile Contents:", repr(file_contents))

        nameHashed =  post_data.split(";")[1].split("=")[1].strip("\"")
        #print("\nName Hashed: ", repr(nameHashed))

        computedHash = sha256(name)


        s = "Appears to be a valid file upload from a client...\n"
        logFH.write(s)
        s = "Name:" + name + "\n"
        logFH.write(s)
        #print(nameHashed)
        #print(computedHash)

        s = "Verifying...\n"
        logFH.write(s)
        if(nameHashed == computedHash):
            s = "\tVerified!\n"
            logFH.write(s)

            s = "\tWriting file...\n"
            logFH.write(s)

            location = writeFile(name, file_contents, 'w')
            s = "\tWrote to: " + str(location) + "\n"
            logFH.write(s)

        else:
            s = "Verification failed!!\n"
            logFH.write(s)

        self.finish_clean(logFH)





    def respond_ok(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def finish_clean(self, logFH):
        logFH.write("\n")
        logFH.close() 
        self.respond_ok()





if __name__ == '__main__':
    httpd = HTTPServer((HOST_NAME, PORT_NUMBER), FileHandler)
    s = str(time.asctime()) +  ' Server Starting - %s:%s' % (HOST_NAME, PORT_NUMBER) + "\n"
    writeFile(LOG_FILE_NAME, s, 'a+')

    try:
        print("Starting service...")
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    s = time.asctime() + ' Server Stopped - %s:%s' % (HOST_NAME, PORT_NUMBER) + "\n"
    writeFile(LOG_FILE_NAME, s, 'a')
