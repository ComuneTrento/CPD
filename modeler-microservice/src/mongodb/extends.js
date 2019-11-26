db.getCollection("schemas").aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
				"_id": "Model.FPMN.Diagram" // {schemaId}
			}
		},

		// Stage 2
		{
			$graphLookup: {
			    from: "schemas",
			    startWith: "Model.FPMN.Diagram", // connectToField value(s) that recursive search starts with
			    connectFromField: "＄extends",
			    connectToField: "_id",
			    as: "＄extends"
			}
		},

		// Stage 3
		{
			$project: {
			    "_id": 1,
			    "＄extends": "$＄extends._id"
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
