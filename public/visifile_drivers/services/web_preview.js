{
    doc_type:       'visifile',
    name:           'webPreview',version: 1,
    max_processes:  1,
    descrption:     'Web Preview base_component_id makes a HTML preview of any document',



    events: {
        "This will return code used to show a preview of a document": {
            on: "preview",
            do: async function(args) {
            var promise = new Promise( returnfn => {
                var data_item = args.data_item
                var hash = data_item.hash
                var fullFilePath = getProperty(data_item,"path")
                var extension = getFileExtension(fullFilePath)
                var contentPreviewMethodForDriver = "content_preview_for_" + extension
                findDriverWithMethod(
                    contentPreviewMethodForDriver
                    ,
                                        function(driverName) {

                                            if (driverName) {
                                                //console.log("5) base_component_id:" + driverName)
                                                callDriverMethod( driverName,
                                                                  contentPreviewMethodForDriver,
                                                                  {hash: hash}
                                                            ,
                                                            function(result) {
                                                                if (result.show_as) {
                                                                    findDriverWithMethod(
                                                                        "view_content_as_" + result.show_as
                                                                        ,
                                                                        function(driverName) {

                                                                            if (driverName) {
                                                                                //console.log("5) base_component_id:" + driverName)
                                                                                callDriverMethod( driverName,
                                                                                                  "view_content_as_" + result.show_as,
                                                                                                  {hash: hash}
                                                                                            ,
                                                                                            function(result2) {
                                                                                                    returnfn(result2)
                                                                                                })
                                                                            } else {
                                                                                returnfn({
                                                                                    html: "<div>No viewer for " + result.show_as + "</div>"
                                                                                })

                                                                            }
                                                                        })
                                                                } else {
                                                                    returnfn(result)

                                                                }
                                                            })

                                            } else {
                                                returnfn({
                                                    html: `
                                                        <div>Can not preview</div>
                                                    `
                                                })
                                            }
                                        })
                                })
                                var ret = await promise
                                return ret
            }, end: null
        }

    }

}
