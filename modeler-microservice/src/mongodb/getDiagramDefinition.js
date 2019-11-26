db.getCollection("models").aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  "_id": "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3", // "{diagramId}"
			}
			
			/*
			 *                           model         parent
			 * TODO: diagram -> plane -<                 ^
			 *                           depictions -> model
			 *                                           v
			 *                                         childs
			 */
		},

		// Stage 2
		{
			$project: {
			    "diagram": "$$ROOT"
			}
		},

		// Stage 3
		{
			$lookup: {
			    from: "dis",
			    localField: "diagram._id",
			    foreignField: "modelId",
			    as: "plane"
			}
		},

		// Stage 4
		{
			$unwind: "$plane"
		},

		// Stage 5
		{
			$lookup: {
			    from: "dis",
			    localField: "plane._id",
			    foreignField: "planeId",
			    as: "dis"
			}
		},

		// Stage 6
		{
			$graphLookup: {
			    from: "models",
			    startWith: "$_id",
			    connectFromField: "_id",
			    connectToField: "designId",
			    as: "root",
			    maxDepth: 0,
			    restrictSearchWithMatch: {
			      parentId: {$exists: false}
			    }
			}
		},

		// Stage 7
		{
			$unwind: "$root"
		},

		// Stage 8
		{
			$graphLookup: {
			    from: "models",
			    startWith: "$root._id",
			    connectFromField: "_id",
			    connectToField: "parentId",
			    as: "childs"
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
