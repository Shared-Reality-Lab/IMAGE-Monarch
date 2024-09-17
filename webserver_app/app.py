from flask import Flask, request, jsonify, abort, Response
from flask_cors import CORS, cross_origin
import logging
import hashlib
import json

app = Flask(__name__)
app.config.from_pyfile('config.py')
logging.basicConfig(filename='pattern_design.log', encoding='utf-8', level=logging.DEBUG, format='%(message)s')

CORS(
    app, resources={r"/*": {"origins": "*"}}
)  # CORS allowed for all domains on all routes


class SVG():
    svg_data={}
    svg= {}

svgData=SVG()

@app.route("/render", methods=["POST", "GET"])
@cross_origin()
def render():
    if request.method=="POST":
        req_data=request.get_json()

        if req_data["id"] in svgData.svg_data:
            if (svgData.svg_data[req_data["id"]])["secret"] == req_data["secret"]:
                svgData.svg_data[req_data["id"]] = {"secret":req_data["secret"], "data": req_data["data"], "layer": req_data["layer"]}
                #svgData.svg["data"] = req_data["data"]
                return jsonify("Graphic in channel "+req_data["id"]+" has been updated!")
            else:
                return jsonify("Unauthorized access to existing channel!")
        else:
            #svgData.svg["data"] = req_data["data"]
            svgData.svg_data[req_data["id"]] = {"secret":req_data["secret"], "data": req_data["data"], "layer": req_data["layer"]}
            return jsonify("New channel created with code "+req_data["id"])
        logging.info(req_data["id"])
    if request.method=="GET":
        return jsonify("Hi")
"""
@app.route("/display", methods=["POST", "GET"])
@cross_origin()
@etag
def display():
    if request.method=="GET":
        check_etag(svgData.svg)
        return jsonify(svgData.svg)
"""
@app.route("/display/<id>", methods=["POST", "GET"])
@cross_origin()
def display(id):
    if request.method=="GET":
        if id in svgData.svg_data:
            app.logger.debug(svgData.svg_data[id]["data"])
            response= Response()
            response.mimetype = "application/json"
            response.set_data(json.dumps({"renderings":[{"data":{"graphic":svgData.svg_data[id]["data"], "layer": svgData.svg_data[id]["layer"]}}]}))
            response.add_etag(hashlib.md5((svgData.svg_data[id]["data"]+svgData.svg_data[id]["layer"]).encode()))
            response.make_conditional(request)
            return response
        else:
            return abort(404)

@app.route("/", methods=["POST", "GET"])
@cross_origin()
def home():
    return "Hi" #render_template("buzzer.html", time=datetime.datetime.now().isoformat(), title="TEST")


if __name__ == "__main__":
    app.run(debug=True)