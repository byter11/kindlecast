import os
import shutil
import importlib
import ctypes
import sys
from os.path import dirname, join

pkg_dir = os.path.dirname(__file__)
os.environ["ICU_DATA"] = pkg_dir + "/"
dat_path = os.path.join(pkg_dir, "icudt73l.dat")
os.chmod(dat_path, 0o644)

ctypes.CDLL("libc++_shared.so")
print("C++ runtime loaded.")

preload_libs = ['icu', 'speedup', 'fast_html_entities', 'cPalmdoc']
for lib in preload_libs:
    lib_path = join(dirname(__file__), f'{lib}.cpython-313-aarch64-linux-android.so')
    ctypes.CDLL(lib_path)

    spec = importlib.util.spec_from_file_location(f"calibre_extensions.{lib}", lib_path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[f"calibre_extensions.{lib}"] = module
    spec.loader.exec_module(module)
    print(f"Manual registration of calibre_extensions.{lib} successful!")