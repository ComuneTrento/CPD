db.getCollection("models").aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			    _id: "32585f79-cce0-4aca-86bf-f8df7a641091"
			}
		},

		// Stage 2
		{
			$lookup: {
			    from: "models",
			    localField: "designId",
			    foreignField: "designId",
			    as: "models"
			}
		},

		// Stage 3
		{
			$unwind: "$models"
		},

		// Stage 4
		{
			$match: {
			    "models.＄domain": "Model.FPMN.Interaction.Task",
			    "models.channel": "Model.FPMN.Interaction.Channel.EForm"
			}
		},

		// Stage 5
		{
			$group: {
			    _id: "32585f79-cce0-4aca-86bf-f8df7a641091",
			    "count": {
			      "$sum": 1.0
			    }
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);
