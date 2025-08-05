class Counter:
    def __init__(self):
        self.counter = None;
        # - we do not support parameters at the initialization stage

    def count(self, params, inputs, outputs):
        if (self.counter is None):
            self.counter = params.start;
        else:
            self.counter += 1
        return self.counter


