from setuptools import setup, Extension, find_packages
import os
import shutil


base_dir = os.path.abspath(os.path.dirname(__file__))
lib_dir = os.path.join(base_dir, 'icu4c', 'prebuilt', 'libs', 'android', 'arm64-v8a')
assets_dir = os.path.join(base_dir, 'icu4c', 'prebuilt', 'assets', 'icu')
shutil.copy2(os.path.join(assets_dir, 'icudt73l.dat'), os.path.join(base_dir, 'src', 'calibre_extensions', 'icudt73l.dat'))

icu = Extension(
    'calibre_extensions.icu',
    sources=['calibre/src/calibre/utils/icu.c'],
    libraries=['c++_shared'],
    include_dirs=[os.path.join(base_dir, 'icu4c', 'prebuilt', 'include')],
    # Link ICU statically by providing the full path to .a files
    extra_objects=[
        os.path.join(lib_dir, 'libicui18n_floris.a'),
        os.path.join(lib_dir, 'libicuuc_floris.a'),
        os.path.join(lib_dir, 'libicudata_floris.a'),
        os.path.join(lib_dir, 'libicuio_floris.a'),
    ],
    extra_link_args=[
        '-Wl,--no-as-needed'
    ],
    extra_compile_args=['-DCALIBRE_MODINIT_FUNC=PyMODINIT_FUNC']
)

speedup = Extension(
    'calibre_extensions.speedup',
    sources=['calibre/src/calibre/utils/speedup.c'],
    extra_compile_args=['-DCALIBRE_MODINIT_FUNC=PyMODINIT_FUNC']

)

fast_html_entities = Extension(
    'calibre_extensions.fast_html_entities',
    sources=['calibre/src/calibre/ebooks/html_entities.c'],
    extra_compile_args=['-DCALIBRE_MODINIT_FUNC=PyMODINIT_FUNC']
)

palmdoc = Extension(
    'calibre_extensions.cPalmdoc',
    sources=['calibre/src/calibre/ebooks/compression/palmdoc.c'],
    extra_compile_args=['-DCALIBRE_MODINIT_FUNC=PyMODINIT_FUNC']
)

calibre_src_path = 'calibre/src'
additional_packages = [d for d in os.listdir(calibre_src_path)
                       if os.path.isdir(os.path.join(calibre_src_path, d))]

pkg_dir = {'': 'src'}
for pkg in additional_packages:
    pkg_dir[pkg] = os.path.join(calibre_src_path, pkg)

setup(
    name='calibre_chaquopy',
    version='1.0',
    packages=find_packages(where='src') + find_packages(where='calibre/src'),
    package_dir=pkg_dir,
    package_data={'calibre_extensions': ['*.dat']},
    include_package_data=True,
    description='calibre',
    ext_modules=[icu, palmdoc, speedup, fast_html_entities]
)