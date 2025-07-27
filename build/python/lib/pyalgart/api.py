#  The MIT License (MIT)
#
#  Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

import os
import hashlib
import importlib.util


class Environment:
    def __init__(self):
        self.executor = None
        self.platform = None
        self.working_dir = None

    def import_file(self, file_name, module_name=None):
        """
        Dynamically imports a module with the given name from a file.
        If `file_name` is relative, it is resolved against `working_dir`.

        :param file_name: Path to the Python file, absolute or relative to `working_dir`.
        :param module_name: Optional name to assign to the module.
        :return: Imported module object.
        """
        if not os.path.isabs(file_name):
            if not self.working_dir:
                raise ValueError("working_dir is not set")
            path = os.path.join(self.working_dir, file_name)
        else:
            path = file_name

        if not os.path.isfile(path):
            raise FileNotFoundError(f"Module file not found at path: '{path}'")

        if module_name is None:
            module_name = os.path.splitext(os.path.basename(path))[0]

        spec = importlib.util.spec_from_file_location(module_name, path)
        if spec is None or spec.loader is None:
            raise ImportError(f"Failed to import module '{module_name}' from file '{path}'")

        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

class Parameters:
    def __init__(self):
        self._env = _env;


class Inputs:
    pass


class Outputs:
    pass

_env = Environment()

def env():
    return _env

def import_file(file_name, module_name=None):
    return _env.import_file(file_name, module_name)


