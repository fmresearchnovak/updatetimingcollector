#!/usr/bin/env python3

# backend_service.py
# Author: Ed Novak
# Description:  This is the backend server.  It should be run on
# some machine with a publically facing IP address and it will 

import time
from http.server import BaseHTTPRequestHandler, HTTPServer
import os
import hashlib

HOST_NAME = '155.68.60.102'
PORT_NUMBER = 9000


def sha256(data):
    H = hashlib.sha256()
    H.update(data.encode())
    return H.digest().hex()

class FileHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        print("DO_HEAD!!", self.path)
        self.respond_ok()


    def do_GET(self):
        print("DO_GET!!", self.path)
        self.respond_ok()


    def do_POST(self):
        print()
        print("New POST!!  at:", self.path)

        content_length = int(self.headers['content-length']) # size of data_file
        #post_data = self.rfile.read(content_length)
        post_data = self.rfile.read(content_length).decode("UTF-8")
        #print("posted data:", repr(post_data))

        name = post_data.split("\r\n")[1].split("=")[2].strip("\"")
        #print("\nName: ", repr(name))

        file_contents = post_data.split("\r\n")[4].strip("-")
        #print("\nFile Contents:", repr(file_contents))

        nameHashed =  post_data.split(";")[1].split("=")[1].strip("\"")
        #print("\nName Hashed: ", repr(nameHashed))

        computedHash = sha256(name)


        print("Appears to be a valid file upload from a client...")
        print("Name:", name, "  recieved hash:", repr(nameHashed), "  computed hash:", repr(computedHash))
        #print(nameHashed)
        #print(computedHash)

        print("Verifying...")
        if(nameHashed == computedHash):
            print("\tVerified!")
            print("\tWriting file...")
            location = self.writeFile(name, file_contents)
            print("\tWrote to: ", location)
            self.respond_ok()



    def respond_ok(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def writeFile(self, fileName, c):
        path = "./data/"
        if not os.path.exists(path):
            os.makedirs(path)

        absPath = path + fileName
        fh = open(absPath, "w")
        fh.write(c)
        fh.close()

        return absPath



if __name__ == '__main__':
    httpd = HTTPServer((HOST_NAME, PORT_NUMBER), FileHandler)
    print(time.asctime(), 'Server Starting - %s:%s' % (HOST_NAME, PORT_NUMBER))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print(time.asctime(), 'Server Stopped - %s:%s' % (HOST_NAME, PORT_NUMBER))