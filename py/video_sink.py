import zmq

# cfg
PORT = 5558

# globals
context = None
puller = None

def mk_addr(p):
    return "tcp://*:%d" % (p)

def get_puller():
    global context, puller
    if puller is None:
        context = zmq.Context()
        puller = context.socket(zmq.PULL)
        puller.bind( mk_addr(PORT) )
    return puller

def receive():
    # return get_puller().recv_json()
    return get_puller().recv()

while True:
    msg = receive()
    print 'receiving val: %s' % (msg)
