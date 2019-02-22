import sys, getopt
from . import runForever

def main():
  inport = 0
  outport = 0
  try:
    opts, args = getopt.getopt(sys.argv[1:],"h:i:o:",["iport=","oport="])
  except getopt.GetoptError:
    print('ipcamera -i <inputport> -o <outputport>')
    sys.exit(2)
  for opt, arg in opts:
    if opt == '-h':
      print('ipcamera -i <inputport> -o <outputport>')
      sys.exit()
    elif opt in ("-i", "--iport"):
      inport = int(arg)
    elif opt in ("-o", "--oport"):
      outport = int(arg)
  runForever(inport, outport)
