#!/usr/bin/env python3

# text_file_connecter.py
#
# Copyright 2019 E. Decker
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""A tool to create a single file out of all text files in a directory by
    connecting their contents and saving them in a new file."""

import argparse
import os
from shutil import copy


class FileConnecter(object):
    """Class to connect the text files in a directory; needs at least a string
        representing an output file as argument; the arguments "directory" and
        "encoding" can be "None"."""
    
    def __init__(self, directory, output_file, encoding):
        self.directory = directory
        self.output_file = output_file
        self.enc = encoding
        self.dir_path = self.directory
        
        self.check_paths()
        self.connect_files()
    
    def check_paths(self):
        """Checks and adjusts the paths of the directory and the output file so
            the program can work with them as expected."""
        if self.directory and self.directory[-1:] != '/':
            self.directory = self.directory+'/'
            
        if self.directory is None:
            self.directory = ''
            self.dir_path = None
            
        if self.directory and not '/' in self.output_file:
            self.output_file = self.directory+self.output_file

        # Adjusts the default name of the output file (if the default name was
        # "selected").
        if self.output_file[-20:] == 'unitedFiles_0000.txt':
            count = 0
            while os.path.exists(self.output_file):
                count += 1
                if len(str(count)) == 1:
                    zeros = '000'
                elif len(str(count)) == 2:
                    zeros = '00'
                elif len(str(count)) == 3:
                    zeros = '0'
                else:
                    zeros =''
                self.output_file = self.output_file[:-8]+zeros+str(count)+'.txt'

    def connect_files(self):
        """Connects the content of the files in a directory by copying one file
            and appending the content of the others to it."""
        if os.path.exists(self.output_file):
            print("ERROR: The output file you have selected (\""+self.output_file+"\") already exists!")
            return
        
        print("Conntecting files...")
        no_files = True
        try:
            for file in os.listdir(self.dir_path):
                if '.' in file and not '.py' in file and not self.output_file is file:
                    try:
                        if os.path.exists(self.output_file):
                            # Reads the content of a file and appends it to
                            # "self.output_file"
                            with open(self.directory+file, 'r', encoding=self.enc) as file_to_append_to_output:
                                file_content = file_to_append_to_output.read()
                            with open(self.output_file, 'a', encoding=self.enc) as file_to_get_appended:
                                file_to_get_appended.write(' '+file_content)
                            print("* Content of \""+self.directory+file+"\" successfully appended to the output file (\""+self.output_file+"\").")
                        else:
                            # Copies the first file in the directory as
                            # "self.output_file".
                            copy(self.directory+file, self.output_file)
                            print("* Output file (\""+self.output_file+"\") created and content of \""+self.directory+file+"\" successfully appended to the output file.")
                        no_files = False
                    except PermissionError:
                        print("ERROR: Permission denied (\""+self.directory+file+"\")! File skipped.")
            if no_files:
                print("ERROR: There are no connectable text files in the directory you have selected (\""+self.directory+"\")!")
            else:
                print("File connection(s) successful!")
        except FileNotFoundError:
           print("ERROR: The directory you have selected (\""+self.directory+"\" couldn't be found!")
        except OSError:
            print("ERROR: Invalid argument: The output file couldn't be created! Check if your name for the output file (\"-out\") is acceptable or if its directory exists!")


if __name__ == '__main__':
    
    parser = argparse.ArgumentParser()
    parser.add_argument('-dir', default=None, type=str,
                        help='directory containing the text files you want to connect')
    parser.add_argument('-out', default='unitedFiles_0000.txt', type=str,
                        help='output file')
    parser.add_argument('-utf8', default=None, type=str,
                        help='if the text files you want to connect are UTF-8 encoded')
    args = parser.parse_args()
        
    if args.utf8:
        encoding = 'utf-8'
    else:
        encoding = None

    FileConnecter(args.dir, args.out, encoding)
