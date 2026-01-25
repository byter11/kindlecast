from __future__ import annotations
import sys
import os
import io

class AndroidSafeBytesIO(io.BytesIO):
    def __len__(self):
        return self.getbuffer().nbytes


def noop(*a, **k): pass

def setup():
    sys.run_local = os.path.abspath(__file__)
    base = os.path.dirname(sys.run_local)
    src = os.path.join(base, 'src')
    if src not in sys.path:
        sys.path.insert(0, src)
    sys.resources_location = os.path.join(base, 'resources')
    sys.extensions_location = os.path.join(src, 'calibre', 'plugins')

    import calibre.utils.safe_atexit as safe_atexit
    safe_atexit._send_command = noop

    io.BytesIO = AndroidSafeBytesIO

