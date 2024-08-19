Requests to the [IMAGE-server](https://github.com/Shared-Reality-Lab/IMAGE-server) need to comply with a basic request schema format in order to be processed.

BaseRequestFormat: Specifies generic format of request to IMAGE-server irrespective of graphic type
MapRequestFormat: Extends BaseRequestFormat class for map requests
PhotoRequestFormat: Extends BaseRequestFormat class for photo requests
MakeRequest: Declares functions to be called to make photo or map requests 
ResponseFormat: Specifies format of response from renderers of type ca.mcgill.a11y.image.renderer.TactileSVG