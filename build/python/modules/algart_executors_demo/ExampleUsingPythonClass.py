class Counter:
    def __init__(self):
        self.counter = 0

    def count(self, params, inputs, outputs):
        self.counter += 1
        return self.counter


