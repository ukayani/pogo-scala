name := "pogo"

version := "1.0"

scalaVersion := "2.11.8"

import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

PB.runProtoc in PB.protobufConfig := (args =>
  com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

scalaSource in PB.protobufConfig := sourceManaged.value