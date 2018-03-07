#!/usr/bin/env python3

# backend_service.py
# Author: Ed Novak
# Description:  This is the backend server.  It should be run on
# some machine with a publically facing IP address and it will 

import time
from http.server import BaseHTTPRequestHandler, HTTPServer
import os

HOST_NAME = '155.68.60.102'
PORT_NUMBER = 9000


class FileHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        print("DO_HEAD!!", self.path)
        self.respond_ok()


    def do_GET(self):
        print("DO_GET!!", self.path)
        self.respond_ok()


    def do_POST(self):
        print("DO_POST!!", self.path)

        content_length = int(self.headers['content-length']) # size of data_file
        #post_data = self.rfile.read(content_length)
        post_data = self.rfile.read(content_length).decode("UTF-8")

        #post_data = post_data.split()
        print(repr(post_data))

        file_contents = post_data.split("\r\n")[4].strip("-")
        #print("file contents:", repr(file_contents))

        name =  post_data.split(";")[1].split("=")[1].strip("\"")
        #print("Name: ", repr(name))

        self.writeFile(name, file_contents)
        self.respond_ok()


    def respond_ok(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def writeFile(self, fileName, c):
        fh = open(fileName, "w")
        fh.write(c)
        fh.close()



if __name__ == '__main__':
    httpd = HTTPServer((HOST_NAME, PORT_NUMBER), FileHandler)
    print(time.asctime(), 'Server Starting - %s:%s' % (HOST_NAME, PORT_NUMBER))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print(time.asctime(), 'Server Stopped - %s:%s' % (HOST_NAME, PORT_NUMBER))