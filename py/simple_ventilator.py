import zmq

BASE = "http://www.google.com/search"
QUERY = "q=site:youtube.com+youtube.com/channels/UC"
NUM = "num=100"

# cfg
URIS = [
    "%s?%s&%s&start=%d" % (BASE, QUERY, NUM, 0),
    "%s?%s&%s&start=%d" % (BASE, QUERY, NUM, 100)
]
PORT = 6000

# globals
context = None
pusher = None

def mk_addr(p):
    return "tcp://*:%d" % (p)

def get_pusher():
    global context, pusher
    if pusher is None:
        context = zmq.Context()
        pusher = context.socket(zmq.PUSH)
        pusher.bind( mk_addr(PORT) )
    return pusher

for uri in URIS:
    print "sending: %s" % (uri)
    get_pusher().send(uri)
