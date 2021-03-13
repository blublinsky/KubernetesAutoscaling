# Cloud events

Based on this [specification](https://github.com/cloudevents/spec/blob/master/spec.md)
At its core, CloudEvents define a set of metadata about events transferred between systems - the minimal set of attributes needed to route the request to the proper component and to facilitate proper processing of the event by that component:

* Id - identifies the event, for example: "A234-1234-1234".
* Source - identifies the context in which an event happened, for example: "https://github.com/cloudevents","mailto:cncf-wg-serverless@lists.cncf.io", "urn:uuid:6e8bc430-9c3a-11d9-9669-0800200c9a66", "cloudevents/spec/pull/123".
* Specversion - identifies the version of the CloudEvents specification which the event uses, for example: "1.x-wip".
* Type - describes the type of event related to the originating occurrence, for example: "com.github.pull_request.opened", "com.example.object.deleted.v2".
* Datacontenttype - defines the content type of the data value which must adhere to the RFC2046 format, for example: "text/xml", "application/json", "image/png".
* Dataschema - identifies the schema that data adheres to, for example: "$ref": "#/definitions/dataschemadef".
* Subject - describes the subject of the event in the context of the event producer (identified by source), for example: "mynewfile.jpg".
* Time - a timestamp of when the occurrence happened which must adhere to RFC 3339, for example: "2018-04-05T17:31:00Z".
* Data - contains the event payload, for example: "".
* Data_base64 - contains the base64 encoded event payload, which must adhere to RFC4648, for example: "Zm9vYg==".
* Extensions - add a key/value map to the event content, where key is a string and value is any object. Extension attributes to the CloudEvent specification are meant to be additional metadata that needs to be included to help ensure proper routing and processing of the CloudEvent.

Here the attributes id, source, specversion and type are required, and the rest are optional.

Protocol Buffers Event Format for CloudEvents - Version 0.2 is defined [here](https://github.com/alanconway/cloudevents-spec/blob/master/protobuf-format.md)
along with java example of its usage

````
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;


CloudEventMap event = CloudEventMap.newBuilder()
  .putValue(
    "type",
    CloudEventAny.newBuilder()
      .setStringValue("com.example.emitter.event")
      .build())
  .putValue(
    "specversion",
    CloudEventAny.newBuilder()
      .setStringValue("0.2")
      .build())
  .putValue(
    "time",
    CloudEventAny.newBuilder()
      .setStringValue("2018-10-25T00:00:00+00:00")
      .build())
  .putValue(
    "source",
    CloudEventAny.newBuilder()
      .setStringValue("com.example.source.host1")
      .build())
  .putValue(
    "comExampleCustomextension",
    CloudEventAny.newBuilder()
      .setStringValue("some value for the extension")
      .build())
  .putValue(
    "data",
    CloudEventAny.newBuilder()
      .setBinaryValue(ByteString.copyFrom("a binary string", Charsets.UTF_8))
      .build())
  .build();
````