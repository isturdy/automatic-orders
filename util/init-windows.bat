cd %~dp0
cd ..
SET starsector_dir=C:\Program Files (x86)\Fractal Softworks\Starsector
mklink %cd%\libs\starfarer\starfarer-api\1.0\starfarer-api-1.0.jar "%starsector_dir%\starsector-core\starfarer.api.jar"
mklink %cd%\libs\starfarer\starfarer-api\1.0\starfarer-api-1.0-sources.jar "%starsector_dir%\starsector-core\starfarer.api.zip"
mklink /D "%starsector_dir%\mods\automatic-orders" %cd%\mod