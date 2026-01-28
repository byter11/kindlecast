from __future__ import annotations
import sys
import os
import io
from PIL import Image

class AndroidSafeBytesIO(io.BytesIO):
    def __len__(self):
        return self.getbuffer().nbytes


def noop(*a, **k): pass

def rescale_image(data, maxsizeb=16*1024, dimen=None):
    from calibre.ebooks.mobi import MAX_THUMB_DIMEN
    img = Image.open(io.BytesIO(data))
    data = io.BytesIO()
    img.thumbnail(MAX_THUMB_DIMEN)
    img.save(data, format='JPEG')

    scale = 0.9
    while len(data) > maxsizeb and scale >= 0.05 and scale * img.width >= 1:
        print(f"Compressing image, scaling to {scale:.2f}x")
        img = Image.open(data)
        img.thumbnail((int(scale*img.width), int(scale*img.height)))

        data = io.BytesIO()
        img.save(data, format='JPEG')
        scale -= 0.05

    return data.getvalue()

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

    # Patch rescale_image to use PIL instead of QtGui.QImage
    import calibre.ebooks.mobi.utils as mobi_utils
    mobi_utils.rescale_image = rescale_image

    io.BytesIO = AndroidSafeBytesIO

